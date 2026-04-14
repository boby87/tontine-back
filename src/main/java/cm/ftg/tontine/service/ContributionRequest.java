package cm.ftg.tontine.service;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record ContributionRequest(
    @NotNull Long membreId,
    @NotNull Long tontineId,
    @NotNull Long seanceId,
    @NotNull @Positive @Digits(integer = 13, fraction = 2) BigDecimal montant
) {}
