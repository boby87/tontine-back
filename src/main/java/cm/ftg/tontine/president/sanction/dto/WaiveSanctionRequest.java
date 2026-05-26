package cm.ftg.tontine.president.sanction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WaiveSanctionRequest(
        @NotBlank @Size(max = 1000) String reason
) {
}
