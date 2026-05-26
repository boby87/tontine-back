package cm.ftg.tontine.president.membership.dto;

import cm.ftg.tontine.president.membership.enums.MembershipDecision;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MembershipDecisionRequest(
        @NotNull MembershipDecision decision,
        @Size(max = 2000) String comment
) {
}
