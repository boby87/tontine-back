package cm.ftg.tontine.auditor.control.dto;

import cm.ftg.tontine.auditor.control.enums.CheckpointCategory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public record CompleteControlRequest(
        @NotEmpty @Valid List<CheckpointInput> checkpoints,
        @Size(max = 4000) String observations
) {

    public record CheckpointInput(
            @NotBlank @Size(max = 200) String label,
            @NotNull CheckpointCategory category,
            BigDecimal expectedValue,
            BigDecimal observedValue,
            @Size(max = 2000) String note
    ) {
    }
}
