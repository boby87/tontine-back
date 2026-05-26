package cm.ftg.tontine.censor.contestation.dto;

import cm.ftg.tontine.censor.contestation.enums.ContestationDecision;
import jakarta.validation.constraints.NotNull;

public record DecideContestationRequest(
        @NotNull ContestationDecision decision,
        String comment
) {
}
