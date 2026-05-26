package cm.ftg.tontine.tontine.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TontineRulesEmbeddable {

    @Column(name = "rule_late_penalty", precision = 19, scale = 2, nullable = false)
    private BigDecimal latePenaltyAmount;

    @Column(name = "rule_absence_penalty", precision = 19, scale = 2, nullable = false)
    private BigDecimal absencePenaltyAmount;

    @Column(name = "rule_contrib_late_penalty", precision = 19, scale = 2, nullable = false)
    private BigDecimal contributionLatePenaltyAmount;

    @Column(name = "rule_loan_max_amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal loanMaxAmount;

    @Column(name = "rule_loan_interest_pct", precision = 7, scale = 2, nullable = false)
    private BigDecimal loanInterestRatePercent;

    @Column(name = "rule_loan_max_duration_months", nullable = false)
    private Integer loanMaxDurationMonths;

    @Column(name = "rule_expense_cap_no_validation", precision = 19, scale = 2, nullable = false)
    private BigDecimal expenseCapWithoutValidation;

    @Column(name = "rule_emergency_deduction_pct", precision = 7, scale = 2, nullable = false)
    private BigDecimal emergencyDeductionPercent;

    @Column(name = "rule_operations_deduction_pct", precision = 7, scale = 2, nullable = false)
    private BigDecimal operationsDeductionPercent;
}
