package cm.ftg.tontine.auditor.control.dto;

import cm.ftg.tontine.auditor.control.enums.ControlKind;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CreateControlRequest(
        @NotNull ControlKind kind,
        @NotNull LocalDate periodFrom,
        @NotNull LocalDate periodTo
) {
}
