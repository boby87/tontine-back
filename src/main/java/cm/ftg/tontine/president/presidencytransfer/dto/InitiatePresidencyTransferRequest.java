package cm.ftg.tontine.president.presidencytransfer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record InitiatePresidencyTransferRequest(
        @NotNull UUID targetMemberId,
        @NotBlank @Size(min = 10, max = 2000) String reason
) {
}
