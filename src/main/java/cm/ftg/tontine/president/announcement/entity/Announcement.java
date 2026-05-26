package cm.ftg.tontine.president.announcement.entity;

import cm.ftg.tontine.common.enums.NotificationChannel;
import cm.ftg.tontine.president.announcement.enums.AnnouncementAudience;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "announcements", indexes = {
        @Index(name = "idx_announcement_tontine", columnList = "tontine_id"),
        @Index(name = "idx_announcement_published_at", columnList = "published_at")
})
@Getter
@Setter
@NoArgsConstructor
public class Announcement {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tontine_id", nullable = false)
    private UUID tontineId;

    @Column(name = "author_user_id", nullable = false)
    private UUID authorUserId;

    @Column(name = "author_full_name", nullable = false, length = 160)
    private String authorFullName;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 5000)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AnnouncementAudience audience = AnnouncementAudience.ALL;

    @ElementCollection(fetch = FetchType.EAGER, targetClass = NotificationChannel.class)
    @CollectionTable(name = "announcement_channels",
            joinColumns = @JoinColumn(name = "announcement_id"),
            indexes = @Index(name = "idx_announcement_channel_aid", columnList = "announcement_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    private Set<NotificationChannel> channels = EnumSet.noneOf(NotificationChannel.class);

    @CreationTimestamp
    @Column(name = "published_at", updatable = false, nullable = false)
    private Instant publishedAt;
}
