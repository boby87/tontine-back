package cm.ftg.tontine.president.cycleclose.dto;

import cm.ftg.tontine.president.cycleclose.enums.NextCycleDrawMode;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record SignCycleCloseRequest(
        @NotNull LocalDate nextCycleStartDate,
        @NotNull NextCycleDrawMode drawMode
) {
}
