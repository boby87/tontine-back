package cm.ftg.tontine.president.presidencytransfer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeclinePresidencyTransferRequest(
        @NotBlank @Size(min = 10, max = 1000) String reason
) {
}
