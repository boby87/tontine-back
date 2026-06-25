package cm.ftg.tontine.treasurer.loan.dto;

import cm.ftg.tontine.treasurer.loan.entity.LoanGuarantor;
import cm.ftg.tontine.treasurer.loan.enums.GuarantorStatus;
import java.time.Instant;
import java.util.UUID;

public record LoanGuarantorDto(
        UUID id,
        UUID loanId,
        UUID guarantorId,
        GuarantorStatus status,
        Instant createdAt,
        Instant respondedAt,
        String rejectionReason
) {
    public static LoanGuarantorDto from(LoanGuarantor g) {
        return new LoanGuarantorDto(
                g.getId(),
                g.getLoanId(),
                g.getGuarantorId(),
                g.getStatus(),
                g.getCreatedAt(),
                g.getRespondedAt(),
                g.getRejectionReason()
        );
    }
}
