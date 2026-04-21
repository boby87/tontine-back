package cm.ftg.tontine.service;

import java.time.LocalDateTime;

/**
 * Résultat de la validation d'un token d'invitation.
 */
public record InvitationValidationResult(
    String invitationId,
    Long tontineId,
    String tontineNom,
    int currentUses,
    int maxUses,
    LocalDateTime expiresAt
) {}

