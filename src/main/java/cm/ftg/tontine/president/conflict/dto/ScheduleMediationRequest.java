package cm.ftg.tontine.president.conflict.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record ScheduleMediationRequest(
        @NotNull Instant scheduledAt,
        @Size(max = 2000) String note
) {
}
