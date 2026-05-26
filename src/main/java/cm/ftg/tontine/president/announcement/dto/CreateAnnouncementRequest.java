package cm.ftg.tontine.president.announcement.dto;

import cm.ftg.tontine.common.enums.NotificationChannel;
import cm.ftg.tontine.president.announcement.enums.AnnouncementAudience;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record CreateAnnouncementRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 5000) String body,
        @NotNull AnnouncementAudience audience,
        @NotEmpty Set<NotificationChannel> channels
) {
}
