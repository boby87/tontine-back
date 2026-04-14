package cm.ftg.tontine.service;

import cm.ftg.tontine.domain.ContributionFrequency;
import cm.ftg.tontine.domain.DistributionMode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Résultat immuable de la création d'une tontine.
 * Jamais d'entité JPA exposée — uniquement ce record.
 */
public record TontineResult(
    Long tontineId,
    String nom,
    String description,
    BigDecimal montantCotisation,
    ContributionFrequency contributionFrequency,
    DistributionMode distributionMode,
    Integer cycleSessionsCount,
    String createdByUserId,
    String presidentMemberId,
    LocalDateTime createdAt
) {}
