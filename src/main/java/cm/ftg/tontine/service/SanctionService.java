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
import java.util.concurrent.ExecutorService;

/**
 * Service métier pour la gestion des sanctions — CDC Section 4.3.
 * <p>
 * Processus CDC : Censeur constate infraction → Applique sanction → Notification au membre → Paiement → Soldée.
 * <p>
 * Contraintes appliquées :
 * <ul>
 *   <li><b>Sécurité</b> : rôle CENSEUR vérifié manuellement via TontineMember (en attente de Spring Security)</li>
 *   <li><b>Logique métier</b> : session en cours (IN_PROGRESS), membre ACTIF, montant positif, idempotence</li>
 *   <li><b>Persistance</b> : @Transactional SERIALIZABLE + @Retryable + BigDecimal</li>
 *   <li><b>Modernité</b> : audit/notification asynchrone post-commit via Virtual Thread</li>
 * </ul>
 */
@Service
public class SanctionService {

    private static final Logger log = LoggerFactory.getLogger(SanctionService.class);

    private final SanctionRepository sanctionRepository;
    private final TontineMemberRepository tontineMemberRepository;
    private final SessionRepository sessionRepository;
    private final NotificationService notificationService;
    private final ExecutorService notificationExecutor;

    public SanctionService(SanctionRepository sanctionRepository,
                           TontineMemberRepository tontineMemberRepository,
                           SessionRepository sessionRepository,
                           NotificationService notificationService,
                           ExecutorService notificationExecutor) {
        this.sanctionRepository = sanctionRepository;
        this.tontineMemberRepository = tontineMemberRepository;
        this.sessionRepository = sessionRepository;
        this.notificationService = notificationService;
        this.notificationExecutor = notificationExecutor;
    }

    // ──────────────────────────────────────────────
    //  Appliquer une sanction (CDC §4.3.2)
    // ──────────────────────────────────────────────

    /**
     * Applique une sanction à un membre pour une séance en cours.
     * <p>
     * Pré-conditions :
     * <ul>
     *   <li>Le membre cible ({@code memberId}) existe et est ACTIF</li>
     *   <li>La séance ({@code sessionId}) est en état IN_PROGRESS</li>
     *   <li>Le montant est strictement positif (BigDecimal)</li>
     *   <li>Aucune sanction du même type n'existe déjà pour ce membre/séance (idempotence)</li>
     * </ul>
     *
     * @param memberId  identifiant UUID du TontineMember sanctionné
     * @param sessionId identifiant de la séance en cours
     * @param amount    montant de la sanction (BigDecimal, strictement positif)
     * @param type      type de sanction (CDC §4.3.1)
     * @param reason    motif libre
     * @param censeurMemberId identifiant du censeur qui applique la sanction
     * @return SanctionResult immuable
     */
    @Retryable(
        retryFor = org.springframework.dao.CannotAcquireLockException.class,
        maxAttempts = 3,
        backoff = @Backoff(delay = 100)
    )
    @Transactional(isolation = Isolation.SERIALIZABLE, rollbackFor = Exception.class)
    public SanctionResult applySanction(String memberId,
                                        Long sessionId,
                                        BigDecimal amount,
                                        SanctionType type,
                                        String reason,
                                        String censeurMemberId) {

        // ── Étape 1 : Pré-validation ──

        verifierMontantPositif(amount);
        var member = verifierMembreExisteEtActif(memberId);
        var session = verifierSessionEnCours(sessionId);
        verifierCenseurAutorise(censeurMemberId, member.getTontine().getId());

        var cleIdempotence = genererCleIdempotence(memberId, sessionId, type);
        verifierPasDeDoubleSanction(cleIdempotence);

        // ── Étape 2 : Construction de l'entité (BigDecimal avec scale) ──

        var sanction = new Sanction();
        sanction.setCleIdempotence(cleIdempotence);
        sanction.setMember(member);
        sanction.setSession(session);
        sanction.setType(type);
        sanction.setMontant(amount.setScale(2, RoundingMode.HALF_UP));
        sanction.setMotif(reason);
        sanction.setStatus(SanctionStatus.IMPAYEE);
        sanction.setAppliedByMemberId(censeurMemberId);

        // ── Étape 3 : Persistance atomique ──

        sanction = sanctionRepository.save(sanction);

        // ── Étape 4 : Résultat immuable ──

        var result = new SanctionResult(
            sanction.getId(),
            member.getId(),
            member.getUser().getFirstName() + " " + member.getUser().getLastName(),
            session.getId(),
            member.getTontine().getId(),
            sanction.getType(),
            sanction.getMontant(),
            sanction.getMotif(),
            sanction.getStatus(),
            censeurMemberId,
            sanction.getCleIdempotence(),
            sanction.getCreatedAt()
        );

        // ── Étape 5 : Notification post-commit sur Virtual Thread ──

        enregistrerNotificationPostCommit(result);

        log.info("Sanction appliquée [id={}, type={}, membre={}, session={}, montant=***] — VirtualThread={}",
            sanction.getId(), type, memberId, sessionId,
            Thread.currentThread().isVirtual());

        return result;
    }

    // ──────────────────────────────────────────────
    //  Pré-validations (Skill étape 1)
    // ──────────────────────────────────────────────

    private void verifierMontantPositif(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new MontantInvalideException(BigDecimal.ONE, amount != null ? amount : BigDecimal.ZERO);
        }
    }

    private TontineMember verifierMembreExisteEtActif(String memberId) {
        var member = tontineMemberRepository.findById(memberId)
            .orElseThrow(() -> new MembreTontineIntrouvableException(memberId));
        if (member.getStatus() != TontineMemberStatus.ACTIF) {
            throw new MembreInactifException(memberId);
        }
        return member;
    }

    private Session verifierSessionEnCours(Long sessionId) {
        var session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new SessionNonEnCoursException(sessionId));
        if (session.getStatus() != SessionStatus.IN_PROGRESS) {
            throw new SessionNonEnCoursException(sessionId);
        }
        return session;
    }

    private void verifierCenseurAutorise(String censeurMemberId, Long tontineId) {
        var censeur = tontineMemberRepository.findById(censeurMemberId)
            .orElseThrow(() -> new MembreTontineIntrouvableException(censeurMemberId));
        if (censeur.getRole() != TontineRole.CENSEUR && censeur.getRole() != TontineRole.PRESIDENT) {
            throw new IllegalStateException(
                "Seul un CENSEUR ou PRESIDENT peut appliquer une sanction (rôle actuel : %s)"
                    .formatted(censeur.getRole()));
        }
        if (!censeur.getTontine().getId().equals(tontineId)) {
            throw new IllegalStateException("Le censeur n'appartient pas à la même tontine que le membre sanctionné");
        }
    }

    private String genererCleIdempotence(String memberId, Long sessionId, SanctionType type) {
        return "SAN-%s-%d-%s".formatted(memberId, sessionId, type.name());
    }

    private void verifierPasDeDoubleSanction(String cleIdempotence) {
        if (sanctionRepository.existsByCleIdempotence(cleIdempotence)) {
            throw new DoubleSanctionException(cleIdempotence);
        }
    }

    // ──────────────────────────────────────────────
    //  Notification post-commit (Virtual Thread)
    // ──────────────────────────────────────────────

    private void enregistrerNotificationPostCommit(SanctionResult result) {
        TransactionSynchronizationManager.registerSynchronization(
            new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    notificationExecutor.submit(() ->
                        notificationService.notifierSanctionAppliquee(result));
                }
            }
        );
    }
}
