package cm.ftg.tontine.censor.attendance.dto;

import cm.ftg.tontine.censor.attendance.entity.AttendanceModificationRequest;
import cm.ftg.tontine.censor.attendance.enums.AttendanceModificationStatus;
import cm.ftg.tontine.president.session.enums.AttendanceStatus;
import java.time.Instant;
import java.util.UUID;

public record AttendanceModificationRequestDto(
        UUID id,
        UUID tontineId,
        UUID sessionId,
        Integer sessionNumber,
        UUID memberId,
        String memberFullName,
        AttendanceStatus fromStatus,
        AttendanceStatus toStatus,
        String reason,
        UUID requestedByUserId,
        String requestedByFullName,
        Instant requestedAt,
        AttendanceModificationStatus status,
        String decisionComment,
        Instant decidedAt,
        UUID linkedSanctionId,
        String infoRequest,
        String infoResponse
) {

    public static AttendanceModificationRequestDto from(AttendanceModificationRequest r) {
        return new AttendanceModificationRequestDto(
                r.getId(), r.getTontineId(), r.getSessionId(), r.getSessionNumber(),
                r.getMemberId(), r.getMemberFullName(), r.getFromStatus(), r.getToStatus(),
                r.getReason(), r.getRequestedByUserId(), r.getRequestedByFullName(),
                r.getRequestedAt(), r.getStatus(), r.getDecisionComment(), r.getDecidedAt(),
                r.getLinkedSanctionId(), r.getInfoRequest(), r.getInfoResponse());
    }
}
