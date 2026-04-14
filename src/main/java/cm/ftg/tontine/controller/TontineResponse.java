package cm.ftg.tontine.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TontineResponse(
    Long id,
    String nom,
    String description,
    BigDecimal montantCotisation,
    String contributionFrequency,
    String distributionMode,
    Integer cycleSessionsCount,
    String createdByUserId,
    String presidentMemberId,
    LocalDateTime createdAt
) {}
