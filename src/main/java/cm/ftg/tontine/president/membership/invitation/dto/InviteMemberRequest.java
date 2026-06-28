package cm.ftg.tontine.president.membership.invitation.dto;

import cm.ftg.tontine.common.enums.UserRole;
import cm.ftg.tontine.president.membership.invitation.enums.InvitationChannel;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record InviteMemberRequest(
        @NotBlank @Size(max = 160) String candidateFullName,
        @NotBlank @Pattern(regexp = "^\\+[1-9]\\d{6,14}$", message = "Le telephone doit etre au format E.164 (ex: +237699000000)") String candidatePhone,
        @Email @Size(max = 160) String candidateEmail,
        @NotNull UserRole proposedRole,
        @NotEmpty Set<InvitationChannel> channels,
        @Size(max = 1000) String message
) {
}
