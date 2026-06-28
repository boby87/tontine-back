package cm.ftg.tontine.president.membership.invitation.dto;

import cm.ftg.tontine.common.enums.UserRole;
import cm.ftg.tontine.president.membership.invitation.entity.MembershipInvitation;
import cm.ftg.tontine.president.membership.invitation.enums.InvitationChannel;
import cm.ftg.tontine.president.membership.invitation.enums.InvitationStatus;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public record MembershipInvitationDto(
        UUID id,
        UUID tontineId,
        String candidateFullName,
        String candidatePhone,
        String candidateEmail,
        UserRole proposedRole,
        List<InvitationChannel> channels,
        String message,
        InvitationStatus status,
        Instant sentAt,
        Instant acceptedAt,
        Instant expiresAt,
        UUID invitedByUserId,
        String invitedByFullName,
        Instant invitedAt,
        int remindersSent,
        Instant cancelledAt,
        String cancelReason,
        String acceptUrl
) {

    public static MembershipInvitationDto from(MembershipInvitation i, String baseAcceptUrl) {
        return new MembershipInvitationDto(
                i.getId(), i.getTontineId(),
                i.getCandidateFullName(), i.getCandidatePhone(), i.getCandidateEmail(),
                i.getProposedRole(),
                Arrays.stream(i.getChannels().split(","))
                        .map(String::trim).filter(s -> !s.isEmpty())
                        .map(InvitationChannel::valueOf).toList(),
                i.getMessage(), i.getStatus(),
                i.getSentAt(), i.getAcceptedAt(), i.getExpiresAt(),
                i.getInvitedByUserId(), i.getInvitedByFullName(), i.getInvitedAt(),
                i.getRemindersSent(),
                i.getCancelledAt(), i.getCancelReason(),
                baseAcceptUrl + "/invitations/" + i.getToken() + "/accept");
    }
}
