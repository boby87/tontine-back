package cm.ftg.tontine.treasurer.loan.dto;

import cm.ftg.tontine.treasurer.loan.entity.LoanRepaymentSchedule;
import cm.ftg.tontine.treasurer.loan.enums.LoanRepaymentScheduleStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record LoanRepaymentScheduleDto(
        UUID id,
        UUID loanId,
        UUID tontineId,
        int installmentNumber,
        Instant dueDate,
        BigDecimal principalComponent,
        BigDecimal interestComponent,
        BigDecimal totalAmount,
        BigDecimal paidAmount,
        LoanRepaymentScheduleStatus status
) {
    public static LoanRepaymentScheduleDto from(LoanRepaymentSchedule s) {
        LoanRepaymentScheduleStatus effective = s.getStatus();
        if ((effective == LoanRepaymentScheduleStatus.PENDING || effective == LoanRepaymentScheduleStatus.PARTIALLY_PAID)
                && s.getDueDate() != null && s.getDueDate().isBefore(Instant.now())) {
            effective = LoanRepaymentScheduleStatus.OVERDUE;
        }
        return new LoanRepaymentScheduleDto(
                s.getId(), s.getLoanId(), s.getTontineId(),
                s.getInstallmentNumber(), s.getDueDate(),
                s.getPrincipalComponent(), s.getInterestComponent(),
                s.getTotalAmount(), s.getPaidAmount(), effective);
    }
}
