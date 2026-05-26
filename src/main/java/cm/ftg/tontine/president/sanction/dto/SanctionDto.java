package cm.ftg.tontine.president.sanction.dto;

import cm.ftg.tontine.president.sanction.entity.Sanction;
import cm.ftg.tontine.president.sanction.enums.SanctionCancelByRole;
import cm.ftg.tontine.president.sanction.enums.SanctionSeverity;
import cm.ftg.tontine.president.sanction.enums.SanctionStatus;
import cm.ftg.tontine.president.sanction.enums.SanctionType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SanctionDto(
        UUID id,
        UUID tontineId,
        UUID memberId,
        String memberFullName,
        UUID sessionId,
        Integer sessionNumber,
        SanctionType type,
        String customLabel,
        BigDecimal amount,
        boolean financial,
        SanctionSeverity severity,
        String reason,
        SanctionStatus status,
        boolean autoDetected,
        UUID issuedByUserId,
        String issuedByFullName,
        Instant issuedAt,
        Instant paidAt,
        Instant contestedAt,
        String contestReason,
        Instant cancelledAt,
        String cancelReason,
        SanctionCancelByRole cancelledByRole,
        boolean refundInitiated
) {

    public static SanctionDto from(Sanction s) {
        return new SanctionDto(
                s.getId(), s.getTontineId(), s.getMemberId(), s.getMemberFullName(),
                s.getSessionId(), s.getSessionNumber(), s.getType(), s.getCustomLabel(),
                s.getAmount(), s.isFinancial(), s.getSeverity(), s.getReason(), s.getStatus(),
                s.isAutoDetected(), s.getIssuedByUserId(), s.getIssuedByFullName(), s.getIssuedAt(),
                s.getPaidAt(), s.getContestedAt(), s.getContestReason(),
                s.getCancelledAt(), s.getCancelReason(), s.getCancelledByRole(), s.isRefundInitiated());
    }
}
