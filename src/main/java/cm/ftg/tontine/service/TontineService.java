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

import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;

/**
 * Service métier pour la création et la configuration d'une tontine.
 * Applique le skill financial-logic : atomicité, BigDecimal, validation pré-opératoire.
 */
@Service
public class TontineService {

    private static final Logger log = LoggerFactory.getLogger(TontineService.class);

    private final TontineRepository tontineRepository;
    private final UserRepository userRepository;
    private final TontineMemberRepository tontineMemberRepository;
    private final NotificationService notificationService;
    private final ExecutorService notificationExecutor;

    public TontineService(TontineRepository tontineRepository,
                          UserRepository userRepository,
                          TontineMemberRepository tontineMemberRepository,
                          NotificationService notificationService,
                          ExecutorService notificationExecutor) {
        this.tontineRepository = tontineRepository;
        this.userRepository = userRepository;
        this.tontineMemberRepository = tontineMemberRepository;
        this.notificationService = notificationService;
        this.notificationExecutor = notificationExecutor;
    }

    // ──────────────────────────────────────────────
    //  Création d'une tontine
    // ──────────────────────────────────────────────

    @Retryable(
        retryFor = org.springframework.dao.CannotAcquireLockException.class,
        maxAttempts = 3,
        backoff = @Backoff(delay = 100)
    )
    @Transactional(isolation = Isolation.SERIALIZABLE, rollbackFor = Exception.class)
    public TontineResult creerTontine(CreateTontineRequest request) {

        // ── Étape 1 : Pré-validation ──

        var user = verifierUtilisateurExiste(request.createdByUserId());
        verifierNomUnique(request.nom());
        var frequency = parseContributionFrequency(request.contributionFrequency());
        var mode = parseDistributionMode(request.distributionMode());

        // ── Étape 2 : Construction de l'entité (BigDecimal avec scale) ──

        var tontine = new Tontine();
        tontine.setNom(request.nom());
        tontine.setDescription(request.description());
        tontine.setMontantCotisation(
            request.montantCotisation().setScale(2, RoundingMode.HALF_UP));
        tontine.setTauxAmendeForfaitaireJour(
            request.tauxAmendeForfaitaireJour().setScale(2, RoundingMode.HALF_UP));
        tontine.setPlafondAmendeEnPourcentage(
            request.plafondAmendeEnPourcentage().setScale(2, RoundingMode.HALF_UP));
        tontine.setContributionFrequency(frequency);
        tontine.setDistributionMode(mode);
        tontine.setCycleSessionsCount(request.cycleSessionsCount());
        tontine.setCreatedBy(user);

        // ── Étape 3 : Persistance atomique (tontine + membre PRESIDENT) ──

        tontine = tontineRepository.save(tontine);

        var president = new TontineMember();
        president.setTontine(tontine);
        president.setUser(user);
        president.setRole(TontineRole.PRESIDENT);
        president.setStatus(TontineMemberStatus.ACTIF);
        president.setJoinedAt(LocalDateTime.now());
        president.setRotationOrder(1);
        president = tontineMemberRepository.save(president);

        // ── Étape 4 : Résultat immuable ──

        var result = new TontineResult(
            tontine.getId(),
            tontine.getNom(),
            tontine.getDescription(),
            tontine.getMontantCotisation(),
            tontine.getContributionFrequency(),
            tontine.getDistributionMode(),
            tontine.getCycleSessionsCount(),
            user.getId(),
            president.getId(),
            tontine.getCreatedAt()
        );

        // ── Étape 5 : Notification post-commit sur Virtual Thread ──

        enregistrerNotificationPostCommit(result);

        log.info("Tontine créée [id={}, nom={}, créateur={}, mode={}] — VirtualThread={}",
            tontine.getId(), tontine.getNom(), user.getId(), mode,
            Thread.currentThread().isVirtual());

        return result;
    }

    // ──────────────────────────────────────────────
    //  Lecture
    // ──────────────────────────────────────────────

    @Transactional(readOnly = true)
    public TontineResult findById(Long tontineId) {
        var tontine = tontineRepository.findByIdAndDeletedAtIsNull(tontineId)
            .orElseThrow(() -> new TontineIntrouvableException(tontineId));

        return new TontineResult(
            tontine.getId(),
            tontine.getNom(),
            tontine.getDescription(),
            tontine.getMontantCotisation(),
            tontine.getContributionFrequency(),
            tontine.getDistributionMode(),
            tontine.getCycleSessionsCount(),
            tontine.getCreatedBy() != null ? tontine.getCreatedBy().getId() : null,
            null,
            tontine.getCreatedAt()
        );
    }

    // ──────────────────────────────────────────────
    //  Pré-validations (Skill étape 1)
    // ──────────────────────────────────────────────

    private User verifierUtilisateurExiste(String userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new UtilisateurIntrouvableException(userId));
    }

    private void verifierNomUnique(String nom) {
        if (tontineRepository.existsByNom(nom)) {
            throw new NomTontineDejaExistantException(nom);
        }
    }

    private ContributionFrequency parseContributionFrequency(String value) {
        try {
            return ContributionFrequency.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                "Fréquence de cotisation invalide : '%s'. Valeurs acceptées : WEEKLY, BIWEEKLY, MONTHLY, CUSTOM"
                    .formatted(value));
        }
    }

    private DistributionMode parseDistributionMode(String value) {
        try {
            return DistributionMode.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                "Mode de distribution invalide : '%s'. Valeurs acceptées : ROTATION, AUCTION, LOTTERY"
                    .formatted(value));
        }
    }

    // ──────────────────────────────────────────────
    //  Notification post-commit (Virtual Thread)
    // ──────────────────────────────────────────────

    private void enregistrerNotificationPostCommit(TontineResult result) {
        TransactionSynchronizationManager.registerSynchronization(
            new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    notificationExecutor.submit(() ->
                        notificationService.notifierTontineCreee(result));
                }
            }
        );
    }
}
