package cm.ftg.tontine.service;

import cm.ftg.tontine.domain.SanctionStatus;
import cm.ftg.tontine.domain.SanctionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Résultat immuable d'une sanction appliquée.
 * Montant masqué dans les logs — jamais l'entité JPA.
 */
public record SanctionResult(
    String sanctionId,
    String memberId,
    String memberName,
    Long sessionId,
    Long tontineId,
    SanctionType type,
    BigDecimal montant,
    String motif,
    SanctionStatus status,
    String appliedByMemberId,
    String cleIdempotence,
    LocalDateTime createdAt
) {}
