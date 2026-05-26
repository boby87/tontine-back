package cm.ftg.tontine.tontine.dto;

import cm.ftg.tontine.tontine.entity.TontineRulesEmbeddable;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record TontineRulesDto(
        @NotNull @PositiveOrZero BigDecimal latePenaltyAmount,
        @NotNull @PositiveOrZero BigDecimal absencePenaltyAmount,
        @NotNull @PositiveOrZero BigDecimal contributionLatePenaltyAmount,
        @NotNull @Positive BigDecimal loanMaxAmount,
        @NotNull @DecimalMin("0.0") @DecimalMax("100.0") BigDecimal loanInterestRatePercent,
        @NotNull @Min(1) Integer loanMaxDurationMonths,
        @NotNull @PositiveOrZero BigDecimal expenseCapWithoutValidation,
        @NotNull @DecimalMin("0.0") @DecimalMax("100.0") BigDecimal emergencyDeductionPercent,
        @NotNull @DecimalMin("0.0") @DecimalMax("100.0") BigDecimal operationsDeductionPercent
) {

    public TontineRulesEmbeddable toEmbeddable() {
        return TontineRulesEmbeddable.builder()
                .latePenaltyAmount(latePenaltyAmount)
                .absencePenaltyAmount(absencePenaltyAmount)
                .contributionLatePenaltyAmount(contributionLatePenaltyAmount)
                .loanMaxAmount(loanMaxAmount)
                .loanInterestRatePercent(loanInterestRatePercent)
                .loanMaxDurationMonths(loanMaxDurationMonths)
                .expenseCapWithoutValidation(expenseCapWithoutValidation)
                .emergencyDeductionPercent(emergencyDeductionPercent)
                .operationsDeductionPercent(operationsDeductionPercent)
                .build();
    }

    public static TontineRulesDto from(TontineRulesEmbeddable r) {
        if (r == null) {
            return null;
        }
        return new TontineRulesDto(
                r.getLatePenaltyAmount(),
                r.getAbsencePenaltyAmount(),
                r.getContributionLatePenaltyAmount(),
                r.getLoanMaxAmount(),
                r.getLoanInterestRatePercent(),
                r.getLoanMaxDurationMonths(),
                r.getExpenseCapWithoutValidation(),
                r.getEmergencyDeductionPercent(),
                r.getOperationsDeductionPercent());
    }
}
