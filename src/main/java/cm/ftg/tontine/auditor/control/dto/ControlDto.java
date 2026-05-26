package cm.ftg.tontine.auditor.control.dto;

import cm.ftg.tontine.auditor.control.entity.Control;
import cm.ftg.tontine.auditor.control.enums.ControlKind;
import cm.ftg.tontine.auditor.control.enums.ControlStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ControlDto(
        UUID id,
        UUID tontineId,
        ControlKind kind,
        LocalDate periodFrom,
        LocalDate periodTo,
        ControlStatus status,
        String observations,
        UUID createdByUserId,
        String createdByFullName,
        Instant createdAt,
        Instant completedAt,
        List<ControlCheckpointDto> checkpoints
) {

    public static ControlDto from(Control c, List<ControlCheckpointDto> checkpoints) {
        return new ControlDto(
                c.getId(), c.getTontineId(), c.getKind(),
                c.getPeriodFrom(), c.getPeriodTo(), c.getStatus(),
                c.getObservations(), c.getCreatedByUserId(), c.getCreatedByFullName(),
                c.getCreatedAt(), c.getCompletedAt(), checkpoints);
    }
}
