package cm.ftg.tontine.president.validation.dto;

import cm.ftg.tontine.president.validation.enums.DecisionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DecisionRequest(
        @NotNull DecisionType decision,
        @Size(max = 2000) String comment
) {
}
