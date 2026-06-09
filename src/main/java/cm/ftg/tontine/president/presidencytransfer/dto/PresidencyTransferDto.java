package cm.ftg.tontine.president.presidencytransfer.dto;

import cm.ftg.tontine.president.presidencytransfer.entity.PresidencyTransfer;
import cm.ftg.tontine.president.presidencytransfer.enums.PresidencyTransferStatus;
import java.time.Instant;
import java.util.UUID;

public record PresidencyTransferDto(
        UUID id,
        UUID tontineId,
        UUID initiatedByUserId,
        String initiatedByFullName,
        UUID targetMemberId,
        String targetMemberFullName,
        UUID targetUserId,
        String reason,
        PresidencyTransferStatus status,
        Instant initiatedAt,
        Instant expiresAt,
        Instant acceptedAt,
        Instant declinedAt,
        String declineReason,
        Instant cancelledAt,
        String cancelReason
) {

    public static PresidencyTransferDto from(PresidencyTransfer t) {
        return new PresidencyTransferDto(
                t.getId(), t.getTontineId(),
                t.getInitiatedByUserId(), t.getInitiatedByFullName(),
                t.getTargetMemberId(), t.getTargetMemberFullName(), t.getTargetUserId(),
                t.getReason(), t.getStatus(),
                t.getInitiatedAt(), t.getExpiresAt(),
                t.getAcceptedAt(), t.getDeclinedAt(), t.getDeclineReason(),
                t.getCancelledAt(), t.getCancelReason());
    }
}
