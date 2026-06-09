package cm.ftg.tontine.president.membership.invitation.dto;

import cm.ftg.tontine.common.enums.UserRole;
import java.time.Instant;

public record InvitationPreviewDto(
        String tontineName,
        String invitedByFullName,
        UserRole proposedRole,
        String candidateFullName,
        String candidatePhone,
        String candidateEmail,
        Instant expiresAt,
        boolean expired,
        boolean alreadyAccepted
) {
}
