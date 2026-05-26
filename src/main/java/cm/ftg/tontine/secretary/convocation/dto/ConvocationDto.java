package cm.ftg.tontine.secretary.convocation.dto;

import cm.ftg.tontine.secretary.convocation.entity.Convocation;
import cm.ftg.tontine.secretary.convocation.enums.ConvocationChannel;
import cm.ftg.tontine.secretary.convocation.enums.ConvocationStatus;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record ConvocationDto(
        UUID id,
        UUID tontineId,
        UUID sessionId,
        int sessionNumber,
        Instant scheduledFor,
        Set<ConvocationChannel> channels,
        Set<UUID> audienceMemberIds,
        boolean includeCandidates,
        List<ReminderDto> reminders,
        String message,
        ConvocationStatus status,
        Instant sentAt,
        Instant scheduledAt,
        int totalRecipients,
        int totalDelivered,
        int totalFailed
) {

    public static ConvocationDto from(Convocation c) {
        List<ReminderDto> reminders = c.getReminders().stream()
                .map(ReminderDto::from)
                .toList();
        return new ConvocationDto(
                c.getId(),
                c.getTontineId(),
                c.getSessionId(),
                c.getSessionNumber(),
                c.getScheduledFor(),
                c.getChannels(),
                c.getAudienceMemberIds(),
                c.isIncludeCandidates(),
                reminders,
                c.getMessage(),
                c.getStatus(),
                c.getSentAt(),
                c.getScheduledAt(),
                c.getTotalRecipients(),
                c.getTotalDelivered(),
                c.getTotalFailed());
    }
}
