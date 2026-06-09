package cm.ftg.tontine.president.membership.invitation.dto;

import cm.ftg.tontine.auth.dto.AuthSessionDto;
import java.util.UUID;

public record AcceptInvitationResponse(
        AuthSessionDto session,
        UUID memberId,
        UUID tontineId,
        String tontineName
) {
}
