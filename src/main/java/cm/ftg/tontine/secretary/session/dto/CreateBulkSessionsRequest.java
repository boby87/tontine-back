package cm.ftg.tontine.secretary.session.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record CreateBulkSessionsRequest(
        @NotNull UUID cycleId,
        @NotNull @Size(min = 1, max = 52) List<@Valid BulkSessionItem> sessions
) {
}
