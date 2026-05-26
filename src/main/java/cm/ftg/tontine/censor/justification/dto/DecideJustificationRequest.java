package cm.ftg.tontine.censor.justification.dto;

import cm.ftg.tontine.censor.justification.enums.JustificationDecision;
import jakarta.validation.constraints.NotNull;

public record DecideJustificationRequest(
        @NotNull JustificationDecision decision,
        String comment
) {
}
