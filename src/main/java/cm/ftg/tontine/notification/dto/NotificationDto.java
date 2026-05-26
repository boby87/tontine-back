package cm.ftg.tontine.notification.dto;

import cm.ftg.tontine.notification.entity.AppNotification;
import cm.ftg.tontine.notification.enums.NotificationCategory;
import cm.ftg.tontine.notification.enums.NotificationKind;
import java.time.Instant;
import java.util.UUID;

public record NotificationDto(
        UUID id,
        UUID userId,
        UUID tontineId,
        NotificationKind kind,
        NotificationCategory category,
        String title,
        String message,
        String link,
        boolean read,
        Instant readAt,
        Instant createdAt
) {

    public static NotificationDto from(AppNotification n) {
        return new NotificationDto(
                n.getId(), n.getUserId(), n.getTontineId(),
                n.getKind(), n.getCategory(),
                n.getTitle(), n.getMessage(), n.getLink(),
                n.isRead(), n.getReadAt(), n.getCreatedAt());
    }
}
