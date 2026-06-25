package cm.ftg.tontine.secretary.session.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record BulkSessionItem(
        @NotNull Instant scheduledAt,
        @Size(max = 200) String location
) {
}
