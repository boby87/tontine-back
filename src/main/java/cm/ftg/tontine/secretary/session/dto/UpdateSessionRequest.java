package cm.ftg.tontine.secretary.session.dto;

import jakarta.validation.constraints.Size;
import java.time.Instant;

public record UpdateSessionRequest(
        Instant scheduledAt,
        @Size(max = 200) String location
) {
}
