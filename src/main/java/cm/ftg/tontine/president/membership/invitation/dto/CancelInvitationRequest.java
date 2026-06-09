package cm.ftg.tontine.president.membership.invitation.dto;

import jakarta.validation.constraints.Size;

public record CancelInvitationRequest(
        @Size(max = 500) String reason
) {
}
