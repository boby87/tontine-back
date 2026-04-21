package cm.ftg.tontine.service;

import cm.ftg.tontine.domain.TontineMemberStatus;
import cm.ftg.tontine.domain.TontineRole;

import java.time.LocalDateTime;

/**
 * Résultat de l'ajout d'un membre à une tontine.
 */
public record AddMemberResult(
    String memberId,
    Long tontineId,
    String userId,
    String userEmail,
    TontineRole role,
    TontineMemberStatus status,
    LocalDateTime joinedAt
) {}

