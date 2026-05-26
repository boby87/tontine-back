package cm.ftg.tontine.president.announcement.dto;

import cm.ftg.tontine.common.enums.NotificationChannel;
import cm.ftg.tontine.president.announcement.entity.Announcement;
import cm.ftg.tontine.president.announcement.enums.AnnouncementAudience;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

public record AnnouncementDto(
        UUID id,
        UUID tontineId,
        UUID authorUserId,
        String authorFullName,
        String title,
        String body,
        AnnouncementAudience audience,
        Set<NotificationChannel> channels,
        Instant publishedAt
) {

    public static AnnouncementDto from(Announcement a) {
        Set<NotificationChannel> channels = (a.getChannels() == null || a.getChannels().isEmpty())
                ? EnumSet.noneOf(NotificationChannel.class)
                : EnumSet.copyOf(a.getChannels());
        return new AnnouncementDto(
                a.getId(), a.getTontineId(), a.getAuthorUserId(), a.getAuthorFullName(),
                a.getTitle(), a.getBody(), a.getAudience(), channels, a.getPublishedAt());
    }
}
