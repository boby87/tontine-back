package cm.ftg.tontine.president.conflict.dto;

import cm.ftg.tontine.president.conflict.enums.ConflictDecisionOutcome;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ConflictDecisionRequest(
        @NotNull ConflictDecisionOutcome outcome,
        @NotBlank @Size(max = 2000) String comment
) {
}
