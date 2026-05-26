package cm.ftg.tontine.secretary.membership.dto;

import cm.ftg.tontine.secretary.membership.enums.SecretaryMembershipDecision;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SecretaryMembershipReviewRequest(
        @NotNull SecretaryMembershipDecision decision,
        @Size(max = 2000) String comment
) {
}
