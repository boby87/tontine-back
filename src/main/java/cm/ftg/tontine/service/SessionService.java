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

import java.time.LocalDate;
import java.util.concurrent.ExecutorService;

/**
 * Service mÃ©tier pour la planification et l'ouverture de sÃ©ances.
 * Applique le skill financial-logic : atomicitÃ©, validation prÃ©-opÃ©ratoire.
 */
@Service
public class SessionService {

    private static final Logger log = LoggerFactory.getLogger(SessionService.class);

    private final SessionRepository sessionRepository;
    private final TontineRepository tontineRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final ExecutorService notificationExecutor;

    public SessionService(SessionRepository sessionRepository,
                          TontineRepository tontineRepository,
                          UserRepository userRepository,
                          NotificationService notificationService,
                          ExecutorService notificationExecutor) {
        this.sessionRepository = sessionRepository;
        this.tontineRepository = tontineRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.notificationExecutor = notificationExecutor;
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    //  Planification d'une sÃ©ance
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Retryable(
        retryFor = org.springframework.dao.CannotAcquireLockException.class,
        maxAttempts = 3,
        backoff = @Backoff(delay = 100)
    )
    @Transactional(isolation = Isolation.SERIALIZABLE, rollbackFor = Exception.class)
    public SessionResult planifierSession(CreateSessionRequest request) {

        // â”€â”€ Ã‰tape 1 : PrÃ©-validation â”€â”€

        verifierDateFuture(request.scheduledDate());
        var tontine = verifierTontineExiste(request.tontineId());
        var user = verifierUtilisateurExiste(request.createdByUserId());

        // â”€â”€ Ã‰tape 2 : Calcul automatique du numÃ©ro de sÃ©ance â”€â”€

        int nextSessionNumber = sessionRepository.countByTontineId(tontine.getId()) + 1;

        // â”€â”€ Ã‰tape 3 : Persistance atomique â”€â”€

        var session = new Session();
        session.setTontine(tontine);
        session.setSessionNumber(nextSessionNumber);
        session.setScheduledDate(request.scheduledDate());
        session.setStartTime(request.startTime());
        session.setEndTimePlanned(request.endTimePlanned());
        session.setLocationAddress(request.locationAddress());
        session.setAgenda(request.agenda());
        session.setStatus(SessionStatus.SCHEDULED);
        session.setCreatedBy(user);

        session = sessionRepository.save(session);

        // â”€â”€ Ã‰tape 4 : RÃ©sultat immuable â”€â”€

        var result = new SessionResult(
            session.getId(),
            tontine.getId(),
            session.getSessionNumber(),
            session.getScheduledDate(),
            session.getStartTime(),
            session.getEndTimePlanned(),
            session.getLocationAddress(),
            session.getAgenda(),
            session.getStatus(),
            user.getId(),
            session.getCreatedAt()
        );

        // â”€â”€ Ã‰tape 5 : Notification post-commit sur Virtual Thread â”€â”€

        enregistrerNotificationPostCommit(result);

        log.info("Session planifiÃ©e [id={}, tontine={}, nÂ°={}, date={}, crÃ©ateur={}] â€” VirtualThread={}",
            session.getId(), tontine.getId(), nextSessionNumber,
            request.scheduledDate(), user.getId(),
            Thread.currentThread().isVirtual());

        return result;
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    //  Ouverture d'une sÃ©ance
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Transactional(isolation = Isolation.SERIALIZABLE, rollbackFor = Exception.class)
    public SessionResult ouvrirSession(Long sessionId) {

        var session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new SeanceNonOuverteException(sessionId));

        if (session.getStatus() != SessionStatus.SCHEDULED) {
            throw new IllegalStateException(
                "La sÃ©ance %d ne peut pas Ãªtre ouverte (statut actuel : %s)"
                    .formatted(sessionId, session.getStatus()));
        }

        session.setStatus(SessionStatus.IN_PROGRESS);
        session = sessionRepository.save(session);

        log.info("Session ouverte [id={}, tontine={}, statut=IN_PROGRESS] â€” VirtualThread={}",
            session.getId(), session.getTontine().getId(),
            Thread.currentThread().isVirtual());

        return toResult(session);
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    //  Lecture
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Transactional(readOnly = true)
    public SessionResult findById(Long sessionId) {
        var session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new SeanceNonOuverteException(sessionId));
        return toResult(session);
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    //  PrÃ©-validations (Skill Ã©tape 1)
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    void verifierDateFuture(LocalDate date) {
        if (date.isBefore(LocalDate.now())) {
            throw new DateDansLePasseException(date);
        }
    }

    private Tontine verifierTontineExiste(Long tontineId) {
        return tontineRepository.findByIdAndDeletedAtIsNull(tontineId)
            .orElseThrow(() -> new TontineIntrouvableException(tontineId));
    }

    private User verifierUtilisateurExiste(String userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new UtilisateurIntrouvableException(userId));
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    //  Mapping interne
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private SessionResult toResult(Session session) {
        return new SessionResult(
            session.getId(),
            session.getTontine().getId(),
            session.getSessionNumber(),
            session.getScheduledDate(),
            session.getStartTime(),
            session.getEndTimePlanned(),
            session.getLocationAddress(),
            session.getAgenda(),
            session.getStatus(),
            session.getCreatedBy() != null ? session.getCreatedBy().getId() : null,
            session.getCreatedAt()
        );
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    //  Notification post-commit (Virtual Thread)
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private void enregistrerNotificationPostCommit(SessionResult result) {
        TransactionSynchronizationManager.registerSynchronization(
            new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    notificationExecutor.submit(() ->
                        notificationService.notifierSessionPlanifiee(result));
                }
            }
        );
    }
}
