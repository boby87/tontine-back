package cm.ftg.tontine.secretary.rsvp.dto;

import cm.ftg.tontine.secretary.rsvp.entity.SessionRsvp;
import cm.ftg.tontine.secretary.rsvp.enums.RsvpStatus;
import java.time.Instant;
import java.util.UUID;

public record SessionRsvpDto(
        UUID sessionId,
        UUID memberId,
        String memberFullName,
        RsvpStatus status,
        String reason,
        Instant respondedAt
) {

    public static SessionRsvpDto from(SessionRsvp r) {
        return new SessionRsvpDto(
                r.getSessionId(),
                r.getMemberId(),
                r.getMemberFullName(),
                r.getStatus(),
                r.getReason(),
                r.getRespondedAt());
    }

    public static SessionRsvpDto pendingFor(UUID sessionId, UUID memberId, String fullName) {
        return new SessionRsvpDto(sessionId, memberId, fullName, RsvpStatus.PENDING, null, null);
    }
}
