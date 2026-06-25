package cm.ftg.tontine.treasurer.loan.entity;

import cm.ftg.tontine.common.entity.BaseEntity;
import cm.ftg.tontine.treasurer.loan.enums.LoanRepaymentScheduleStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "loan_repayment_schedules", indexes = {
        @Index(name = "idx_schedule_loan", columnList = "loan_id"),
        @Index(name = "idx_schedule_tontine", columnList = "tontine_id"),
        @Index(name = "idx_schedule_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
public class LoanRepaymentSchedule extends BaseEntity {

    @Column(name = "loan_id", nullable = false)
    private UUID loanId;

    @Column(name = "tontine_id", nullable = false)
    private UUID tontineId;

    @Column(name = "installment_number", nullable = false)
    private int installmentNumber;

    @Column(name = "due_date", nullable = false)
    private Instant dueDate;

    @Column(name = "principal_component", precision = 19, scale = 2, nullable = false)
    private BigDecimal principalComponent = BigDecimal.ZERO;

    @Column(name = "interest_component", precision = 19, scale = 2, nullable = false)
    private BigDecimal interestComponent = BigDecimal.ZERO;

    @Column(name = "total_amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "paid_amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LoanRepaymentScheduleStatus status = LoanRepaymentScheduleStatus.PENDING;
}
