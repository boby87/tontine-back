package cm.ftg.tontine.president.emergencyblock.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LiftEmergencyBlockRequest(
        @NotBlank @Size(max = 1000) String reason
) {
}
