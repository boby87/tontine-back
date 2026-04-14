package cm.ftg.tontine.service;

import cm.ftg.tontine.domain.*;
import cm.ftg.tontine.exception.*;
import cm.ftg.tontine.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

@Service
public class ContributionService {

    private static final Logger log = LoggerFactory.getLogger(ContributionService.class);

    private final MembreRepository membreRepository;
    private final SeanceRepository seanceRepository;
    private final CotisationRepository cotisationRepository;
    private final AmendeRepository amendeRepository;
    private final NotificationService notificationService;
    private final ExecutorService notificationExecutor;

    public ContributionService(MembreRepository membreRepository,
                               SeanceRepository seanceRepository,
                               CotisationRepository cotisationRepository,
                               AmendeRepository amendeRepository,
                               NotificationService notificationService,
                               ExecutorService notificationExecutor) {
        this.membreRepository = membreRepository;
        this.seanceRepository = seanceRepository;
        this.cotisationRepository = cotisationRepository;
        this.amendeRepository = amendeRepository;
        this.notificationService = notificationService;
        this.notificationExecutor = notificationExecutor;
    }

    // ──────────────────────────────────────────────
    //  Point d'entrée principal
    // ──────────────────────────────────────────────

    @Retryable(
        retryFor = org.springframework.dao.CannotAcquireLockException.class,
        maxAttempts = 3,
        backoff = @Backoff(delay = 100)
    )
    @Transactional(isolation = Isolation.SERIALIZABLE, rollbackFor = Exception.class)
    public ContributionResult recordContribution(ContributionRequest request) {

        // ── Étape 1 : Pré-validation (existence + solvabilité) ──

        var membre = verifierExistence(request.membreId(), request.tontineId());
        var seance = verifierSeanceOuverte(request.seanceId());
        verifierSolvabilite(membre, seance);
        verifierMontant(request.montant(), seance.getTontine().getMontantCotisation());

        // ── Étape 2 : Calcul automatique d'amende si retard ──

        LocalDate aujourdhui = LocalDate.now();
        long joursRetard = calculerJoursRetard(aujourdhui, seance.getDateLimite());
        BigDecimal montantAmende = BigDecimal.ZERO;

        if (joursRetard > 0) {
            montantAmende = calculerAmende(
                seance.getTontine().getMontantCotisation(),
                seance.getTontine().getTauxAmendeForfaitaireJour(),
                seance.getTontine().getPlafondAmendeEnPourcentage(),
                joursRetard
            );
        }

        // ── Étape 3 : Persistance cotisation + amende (atomique) ──

        String cleIdempotence = "COT-" + request.membreId() + "-" + request.seanceId()
                + "-" + UUID.randomUUID();

        StatutCotisation statut = joursRetard > 0 ? StatutCotisation.EN_RETARD : StatutCotisation.PAYEE;

        var cotisation = Cotisation.creer(membre, seance, request.montant(), statut, cleIdempotence);
        cotisation = cotisationRepository.save(cotisation);

        if (montantAmende.compareTo(BigDecimal.ZERO) > 0) {
            var amende = Amende.creer(membre, seance, montantAmende, joursRetard);
            amendeRepository.save(amende);
        }

        // ── Étape 4 : Construction du résultat (record immuable) ──

        var result = new ContributionResult(
            cotisation.getId(),
            cleIdempotence,
            membre.getId(),
            seance.getId(),
            cotisation.getMontant(),
            statut,
            montantAmende,
            joursRetard,
            cotisation.getDateOperation()
        );

        // ── Étape 5 : Notification post-commit sur Virtual Thread ──

        enregistrerNotificationPostCommit(result);

        log.info("Cotisation enregistrée [clé={}, membre={}, séance={}, statut={}, amende=***] — VirtualThread={}",
            cleIdempotence, membre.getId(), seance.getId(), statut,
            Thread.currentThread().isVirtual());

        return result;
    }

    // ──────────────────────────────────────────────
    //  Lecture (Skill étape 1 — readOnly)
    // ──────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ContributionResult findById(Long cotisationId) {
        var cotisation = cotisationRepository.findById(cotisationId)
            .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                "Cotisation %d introuvable".formatted(cotisationId)));

        return new ContributionResult(
            cotisation.getId(),
            cotisation.getCleIdempotence(),
            cotisation.getMembre().getId(),
            cotisation.getSeance().getId(),
            cotisation.getMontant(),
            cotisation.getStatut(),
            BigDecimal.ZERO,
            0,
            cotisation.getDateOperation()
        );
    }

    // ──────────────────────────────────────────────
    //  Pré-validations (Skill étape 1)
    // ──────────────────────────────────────────────

    private Membre verifierExistence(Long membreId, Long tontineId) {
        var membre = membreRepository.findByIdAndTontineId(membreId, tontineId)
            .orElseThrow(() -> new MembreIntrouvableException(membreId, tontineId));

        if (membre.getStatut() != StatutMembre.ACTIF) {
            throw new MembreInactifException(membreId);
        }
        return membre;
    }

    private Seance verifierSeanceOuverte(Long seanceId) {
        var seance = seanceRepository.findById(seanceId)
            .orElseThrow(() -> new SeanceNonOuverteException(seanceId));

        if (seance.isCloturee()) {
            throw new SeanceNonOuverteException(seanceId);
        }
        return seance;
    }

    private void verifierSolvabilite(Membre membre, Seance seance) {
        if (cotisationRepository.existsByMembreIdAndSeanceId(membre.getId(), seance.getId())) {
            throw new DoubleCotisationException(membre.getId(), seance.getId());
        }
    }

    private void verifierMontant(BigDecimal montantRecu, BigDecimal montantAttendu) {
        if (montantRecu.compareTo(montantAttendu) != 0) {
            throw new MontantInvalideException(montantAttendu, montantRecu);
        }
    }

    // ──────────────────────────────────────────────
    //  Calcul d'amende (règle tontine-expert)
    // ──────────────────────────────────────────────

    long calculerJoursRetard(LocalDate aujourdhui, LocalDate dateLimite) {
        if (aujourdhui.isAfter(dateLimite)) {
            return ChronoUnit.DAYS.between(dateLimite, aujourdhui);
        }
        return 0;
    }

    BigDecimal calculerAmende(BigDecimal montantCotisation,
                              BigDecimal tauxForfaitaireJour,
                              BigDecimal plafondPourcentage,
                              long joursRetard) {

        // Amende brute = taux forfaitaire × jours de retard
        BigDecimal amendeBrute = tauxForfaitaireJour
            .multiply(new BigDecimal(joursRetard))
            .setScale(2, RoundingMode.HALF_UP);

        // Plafond = montantCotisation × plafondPourcentage / 100
        BigDecimal plafond = montantCotisation
            .multiply(plafondPourcentage)
            .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        // Retourner le minimum entre amende brute et plafond
        return amendeBrute.compareTo(plafond) > 0 ? plafond : amendeBrute;
    }

    // ──────────────────────────────────────────────
    //  Notification post-commit (Skill étape 3)
    // ──────────────────────────────────────────────

    private void enregistrerNotificationPostCommit(ContributionResult result) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                notificationExecutor.submit(() -> {
                    log.info("Notification envoyée sur VirtualThread={}", Thread.currentThread().isVirtual());
                    try {
                        notificationService.notifierCotisationRecue(result);
                        if (result.montantAmende().compareTo(BigDecimal.ZERO) > 0) {
                            notificationService.notifierAmendeCalculee(result);
                        }
                    } catch (Exception e) {
                        log.error("Échec notification pour cotisation {} : {}",
                            result.cleIdempotence(), e.getMessage());
                    }
                });
            }
        });
    }
}
