package cm.ftg.tontine.secretary.rsvp.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SessionRsvpSummaryDto(
        UUID sessionId,
        int sessionNumber,
        Instant scheduledAt,
        int totalMembers,
        int confirmed,
        int declined,
        int tentative,
        int pending,
        BigDecimal quorumPercent,
        boolean quorumReached,
        List<SessionRsvpDto> rsvps
) {
}
