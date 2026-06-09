package cm.ftg.tontine.president.membership.invitation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AcceptInvitationRequest(
        @NotBlank @Size(min = 8, max = 100) String password
) {
}
