package cm.ftg.tontine.president.delegation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RevokeDelegationRequest(
        @NotBlank @Size(max = 1000) String reason
) {
}
