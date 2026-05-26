package cm.ftg.tontine.censor.sanction.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

public record ConfirmBatchRequest(
        @NotEmpty List<UUID> sanctionIds
) {}
