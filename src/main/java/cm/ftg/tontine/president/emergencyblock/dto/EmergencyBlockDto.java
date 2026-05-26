package cm.ftg.tontine.president.emergencyblock.dto;

import cm.ftg.tontine.president.emergencyblock.entity.EmergencyBlock;
import cm.ftg.tontine.president.emergencyblock.enums.EmergencyBlockStatus;
import cm.ftg.tontine.president.emergencyblock.enums.EmergencyBlockTarget;
import java.time.Instant;
import java.util.UUID;

public record EmergencyBlockDto(
        UUID id,
        UUID tontineId,
        EmergencyBlockTarget target,
        String targetRef,
        String reason,
        EmergencyBlockStatus status,
        UUID activatedByUserId,
        String activatedByFullName,
        Instant activatedAt,
        UUID liftedByUserId,
        Instant liftedAt,
        String liftReason
) {

    public static EmergencyBlockDto from(EmergencyBlock b) {
        return new EmergencyBlockDto(
                b.getId(), b.getTontineId(), b.getTarget(), b.getTargetRef(), b.getReason(),
                b.getStatus(), b.getActivatedByUserId(), b.getActivatedByFullName(), b.getActivatedAt(),
                b.getLiftedByUserId(), b.getLiftedAt(), b.getLiftReason());
    }
}
