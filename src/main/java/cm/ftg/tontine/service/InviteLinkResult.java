package cm.ftg.tontine.service;

import java.time.LocalDateTime;

/**
 * Résultat de la génération d'un lien d'invitation.
 */
public record InviteLinkResult(
    String inviteId,
    Long tontineId,
    String token,
    LocalDateTime expiresAt,
    Integer maxUses
) {}

