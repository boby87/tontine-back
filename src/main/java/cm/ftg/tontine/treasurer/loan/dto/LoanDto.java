package cm.ftg.tontine.treasurer.loan.dto;

import cm.ftg.tontine.treasurer.loan.entity.Loan;
import cm.ftg.tontine.treasurer.loan.enums.LoanStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record LoanDto(
        UUID id,
        UUID tontineId,
        UUID memberId,
        BigDecimal principal,
        BigDecimal interestRate,
        int durationMonths,
        BigDecimal monthlyPayment,
        BigDecimal totalDue,
        BigDecimal totalRepaid,
        LoanStatus status,
        String purpose,
        List<UUID> guarantorIds,
        Instant requestedAt,
        Instant approvedAt,
        Instant disbursedAt,
        Instant dueDate
) {

    public static LoanDto from(Loan l) {
        return new LoanDto(
                l.getId(), l.getTontineId(), l.getMemberId(), l.getPrincipal(), l.getInterestRate(),
                l.getDurationMonths(), l.getMonthlyPayment(), l.getTotalDue(), l.getTotalRepaid(),
                l.getStatus(), l.getPurpose(), List.of(),
                l.getRequestedAt(), l.getApprovedAt(), l.getDisbursedAt(), l.getDueDate());
    }
}
