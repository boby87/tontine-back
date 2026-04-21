package cm.ftg.tontine.service;

import cm.ftg.tontine.domain.TontineInvitation;
import cm.ftg.tontine.exception.AccesNonAutoriseException;
import cm.ftg.tontine.exception.InvitationInvalideException;
import cm.ftg.tontine.exception.TontineIntrouvableException;
import cm.ftg.tontine.exception.UtilisateurIntrouvableException;
import cm.ftg.tontine.domain.TontineRole;
import cm.ftg.tontine.repository.TontineInvitationRepository;
import cm.ftg.tontine.repository.TontineMemberRepository;
import cm.ftg.tontine.repository.TontineRepository;
import cm.ftg.tontine.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;

/**
 * Service métier pour la gestion des invitations à une tontine.
 * <p>
 * <b>Sécurité :</b> Le token en clair est renvoyé une seule fois au créateur.
 * En base, seul le hash SHA-256 est stocké (même principe qu'un mot de passe).
 * <p>
 * <b>Nettoyage :</b> Les invitations expirées sont purgées périodiquement
 * par {@link InvitationCleanupScheduler}.
 */
@Service
public class TontineInvitationService {

    private static final Logger log = LoggerFactory.getLogger(TontineInvitationService.class);

    /** Longueur du token alphanumérique en clair */
    static final int TOKEN_LENGTH = 32;
    /** Durée de validité en jours */
    static final int EXPIRATION_DAYS = 10;

    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final TontineInvitationRepository invitationRepository;
    private final TontineRepository tontineRepository;
    private final UserRepository userRepository;
    private final TontineMemberRepository tontineMemberRepository;

    public TontineInvitationService(TontineInvitationRepository invitationRepository,
                                     TontineRepository tontineRepository,
                                     UserRepository userRepository,
                                     TontineMemberRepository tontineMemberRepository) {
        this.invitationRepository = invitationRepository;
        this.tontineRepository = tontineRepository;
        this.userRepository = userRepository;
        this.tontineMemberRepository = tontineMemberRepository;
    }

    // ──────────────────────────────────────────────
    //  Génération d'invitation
    // ──────────────────────────────────────────────

    /**
     * Génère un token alphanumérique de 32 caractères, stocke son hash SHA-256
     * en base et retourne le token en clair (une seule fois).
     * Seul le PRESIDENT peut générer une invitation.
     *
     * @param request les paramètres de génération
     * @return le résultat contenant le token en clair (à transmettre au destinataire)
     */
    @Transactional(rollbackFor = Exception.class)
    public InviteLinkResult genererInvitation(GenerateInviteLinkRequest request) {

        var tontine = tontineRepository.findByIdAndDeletedAtIsNull(request.tontineId())
            .orElseThrow(() -> new TontineIntrouvableException(request.tontineId()));

        var user = userRepository.findById(request.requestedByUserId())
            .orElseThrow(() -> new UtilisateurIntrouvableException(request.requestedByUserId()));

        verifierEstPresident(request.tontineId(), request.requestedByUserId());

        // ── Génération du token alphanumérique ──
        String tokenClair = genererTokenAlphanumerique(TOKEN_LENGTH);
        String tokenHash = sha256(tokenClair);

        // ── Persistance (hash uniquement) ──
        var invitation = new TontineInvitation();
        invitation.setTontine(tontine);
        invitation.setTokenHash(tokenHash);
        invitation.setExpiresAt(LocalDateTime.now().plusDays(EXPIRATION_DAYS));
        invitation.setCreatedBy(user);
        invitation.setMaxUses(request.maxUses() != null ? request.maxUses() : 1);

        invitation = invitationRepository.save(invitation);

        log.info("Invitation générée [tontine={}, hash={}…, expiration={}j, maxUses={}] — VirtualThread={}",
            tontine.getId(), tokenHash.substring(0, 8), EXPIRATION_DAYS,
            invitation.getMaxUses(), Thread.currentThread().isVirtual());

        return new InviteLinkResult(
            invitation.getId(),
            tontine.getId(),
            tokenClair,  // renvoyé UNE SEULE FOIS — jamais re-lisible depuis la DB
            invitation.getExpiresAt(),
            invitation.getMaxUses()
        );
    }

    // ──────────────────────────────────────────────
    //  Validation de token
    // ──────────────────────────────────────────────

    /**
     * Valide un token d'invitation : existence, expiration, utilisations restantes.
     *
     * @param token le token en clair reçu du client
     * @return le résultat de validation contenant les informations de la tontine
     * @throws InvitationInvalideException si le token est inconnu, expiré ou épuisé
     */
    @Transactional(rollbackFor = Exception.class)
    public InvitationValidationResult validerToken(String token) {

        String tokenHash = sha256(token);

        var invitation = invitationRepository.findByTokenHash(tokenHash)
            .orElseThrow(() -> new InvitationInvalideException("token inconnu"));

        if (invitation.isExpired()) {
            throw new InvitationInvalideException(
                "token expiré depuis le %s".formatted(invitation.getExpiresAt()));
        }

        if (!invitation.hasRemainingUses()) {
            throw new InvitationInvalideException(
                "nombre maximal d'utilisations atteint (%d/%d)"
                    .formatted(invitation.getCurrentUses(), invitation.getMaxUses()));
        }

        // Incrémenter le compteur d'utilisations
        invitation.incrementUses();
        invitationRepository.save(invitation);

        log.info("Token validé [tontine={}, uses={}/{}] — VirtualThread={}",
            invitation.getTontine().getId(),
            invitation.getCurrentUses(), invitation.getMaxUses(),
            Thread.currentThread().isVirtual());

        return new InvitationValidationResult(
            invitation.getId(),
            invitation.getTontine().getId(),
            invitation.getTontine().getNom(),
            invitation.getCurrentUses(),
            invitation.getMaxUses(),
            invitation.getExpiresAt()
        );
    }

    // ──────────────────────────────────────────────
    //  Nettoyage des invitations expirées
    // ──────────────────────────────────────────────

    /**
     * Supprime toutes les invitations expirées.
     * Appelé par {@link InvitationCleanupScheduler} sur un Virtual Thread.
     *
     * @return le nombre d'invitations supprimées
     */
    @Transactional
    public int supprimerInvitationsExpirees() {
        int count = invitationRepository.deleteExpiredBefore(LocalDateTime.now());
        if (count > 0) {
            log.info("Nettoyage invitations : {} invitation(s) expirée(s) supprimée(s) — VirtualThread={}",
                count, Thread.currentThread().isVirtual());
        }
        return count;
    }

    // ──────────────────────────────────────────────
    //  Utilitaires
    // ──────────────────────────────────────────────

    /**
     * Génère un token alphanumérique cryptographiquement sûr.
     */
    static String genererTokenAlphanumerique(int length) {
        var sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHANUMERIC.charAt(SECURE_RANDOM.nextInt(ALPHANUMERIC.length())));
        }
        return sb.toString();
    }

    /**
     * Calcule le hash SHA-256 d'un token et retourne sa représentation hexadécimale.
     */
    static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 est garanti par la spec Java — ne devrait jamais arriver
            throw new IllegalStateException("SHA-256 non disponible", e);
        }
    }

    private void verifierEstPresident(Long tontineId, String userId) {
        var membre = tontineMemberRepository.findByTontineIdAndUserId(tontineId, userId)
            .orElseThrow(() -> new AccesNonAutoriseException(
                "L'utilisateur %s n'est pas membre de la tontine %d".formatted(userId, tontineId)));
        if (membre.getRole() != TontineRole.PRESIDENT) {
            throw new AccesNonAutoriseException(
                "Seul le PRESIDENT peut générer une invitation (rôle actuel : %s)".formatted(membre.getRole()));
        }
    }
}

