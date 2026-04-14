package cm.ftg.tontine.service;

import cm.ftg.tontine.domain.Region;
import cm.ftg.tontine.domain.User;
import cm.ftg.tontine.exception.EmailDejaExistantException;
import cm.ftg.tontine.exception.TelephoneDejaExistantException;
import cm.ftg.tontine.exception.UtilisateurIntrouvableException;
import cm.ftg.tontine.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.ExecutorService;

/**
 * Service métier pour l'inscription et la gestion des utilisateurs.
 * Applique le skill financial-logic : pré-validation, isolation SERIALIZABLE,
 * notification post-commit sur Virtual Thread.
 */
@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoderService passwordEncoder;
    private final NotificationService notificationService;
    private final ExecutorService notificationExecutor;

    public UserService(UserRepository userRepository,
                       PasswordEncoderService passwordEncoder,
                       NotificationService notificationService,
                       ExecutorService notificationExecutor) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.notificationService = notificationService;
        this.notificationExecutor = notificationExecutor;
    }

    // ──────────────────────────────────────────────
    //  Inscription d'un utilisateur
    // ──────────────────────────────────────────────

    @Retryable(
        retryFor = org.springframework.dao.CannotAcquireLockException.class,
        maxAttempts = 3,
        backoff = @Backoff(delay = 100)
    )
    @Transactional(isolation = Isolation.SERIALIZABLE, rollbackFor = Exception.class)
    public UserResult inscrireUtilisateur(CreateUserRequest request) {

        // ── Étape 1 : Pré-validation (unicité email + téléphone) ──

        verifierEmailUnique(request.email());
        verifierTelephoneUnique(request.phone());

        // ── Étape 2 : Construction de l'entité + hachage mot de passe ──

        var region = parseRegion(request.region());

        var user = new User();
        user.setEmail(request.email());
        user.setPhone(request.phone());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setDateOfBirth(request.dateOfBirth());
        user.setRegion(region);
        user.setCniNumber(request.cniNumber());

        // ── Étape 3 : Persistance atomique ──

        user = userRepository.save(user);

        // ── Étape 4 : Résultat immuable ──

        var result = new UserResult(
            user.getId(),
            user.getEmail(),
            user.getPhone(),
            user.getFirstName(),
            user.getLastName(),
            user.getDateOfBirth(),
            user.getRole(),
            user.getRegion(),
            user.getCniNumber(),
            user.getCreatedAt()
        );

        // ── Étape 5 : Notification post-commit sur Virtual Thread ──

        enregistrerNotificationPostCommit(result);

        log.info("Utilisateur inscrit [id={}, email=***, téléphone=****{}] — VirtualThread={}",
            user.getId(),
            user.getPhone().substring(user.getPhone().length() - 4),
            Thread.currentThread().isVirtual());

        return result;
    }

    // ──────────────────────────────────────────────
    //  Lecture
    // ──────────────────────────────────────────────

    @Transactional(readOnly = true)
    public UserResult findById(String userId) {
        var user = userRepository.findById(userId)
            .orElseThrow(() -> new UtilisateurIntrouvableException(userId));

        return new UserResult(
            user.getId(),
            user.getEmail(),
            user.getPhone(),
            user.getFirstName(),
            user.getLastName(),
            user.getDateOfBirth(),
            user.getRole(),
            user.getRegion(),
            user.getCniNumber(),
            user.getCreatedAt()
        );
    }

    // ──────────────────────────────────────────────
    //  Pré-validations (Skill étape 1)
    // ──────────────────────────────────────────────

    private void verifierEmailUnique(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new EmailDejaExistantException(email);
        }
    }

    private void verifierTelephoneUnique(String phone) {
        if (userRepository.existsByPhone(phone)) {
            throw new TelephoneDejaExistantException(phone);
        }
    }

    private Region parseRegion(String value) {
        try {
            return Region.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                "Région invalide : '%s'. Valeurs acceptées : ADAMAOUA, CENTRE, EST, EXTREME_NORD, LITTORAL, NORD, NORD_OUEST, OUEST, SUD, SUD_OUEST"
                    .formatted(value));
        }
    }

    // ──────────────────────────────────────────────
    //  Notification post-commit (Virtual Thread)
    // ──────────────────────────────────────────────

    private void enregistrerNotificationPostCommit(UserResult result) {
        TransactionSynchronizationManager.registerSynchronization(
            new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    notificationExecutor.submit(() ->
                        notificationService.notifierInscription(result));
                }
            }
        );
    }
}
