package cm.ftg.tontine.president.emergencyblock.dto;

import cm.ftg.tontine.president.emergencyblock.enums.EmergencyBlockTarget;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateEmergencyBlockRequest(
        @NotNull EmergencyBlockTarget target,
        @Size(max = 160) String targetRef,
        @NotBlank @Size(max = 1000) String reason
) {
}
