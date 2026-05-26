package cm.ftg.tontine.auditor.control.dto;

import cm.ftg.tontine.auditor.control.entity.ControlCheckpoint;
import cm.ftg.tontine.auditor.control.enums.CheckpointCategory;
import java.math.BigDecimal;
import java.util.UUID;

public record ControlCheckpointDto(
        UUID id,
        UUID controlId,
        String label,
        CheckpointCategory category,
        BigDecimal expectedValue,
        BigDecimal observedValue,
        BigDecimal variance,
        Boolean conform,
        String note,
        int orderIdx
) {

    public static ControlCheckpointDto from(ControlCheckpoint c) {
        return new ControlCheckpointDto(
                c.getId(), c.getControlId(), c.getLabel(), c.getCategory(),
                c.getExpectedValue(), c.getObservedValue(), c.getVariance(),
                c.getConform(), c.getNote(), c.getOrderIdx());
    }
}
