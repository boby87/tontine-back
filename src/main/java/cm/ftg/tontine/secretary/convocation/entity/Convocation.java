package cm.ftg.tontine.secretary.convocation.entity;

import cm.ftg.tontine.secretary.convocation.enums.ConvocationChannel;
import cm.ftg.tontine.secretary.convocation.enums.ConvocationStatus;
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
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "convocations", indexes = {
        @Index(name = "idx_convocations_tontine", columnList = "tontine_id"),
        @Index(name = "idx_convocations_session", columnList = "session_id"),
        @Index(name = "idx_convocations_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
public class Convocation {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tontine_id", nullable = false)
    private UUID tontineId;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "session_number", nullable = false)
    private int sessionNumber;

    @Column(name = "scheduled_for", nullable = false)
    private Instant scheduledFor;

    @Column(length = 4000, nullable = false)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConvocationStatus status = ConvocationStatus.DRAFT;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Column(name = "total_recipients", nullable = false)
    private int totalRecipients = 0;

    @Column(name = "total_delivered", nullable = false)
    private int totalDelivered = 0;

    @Column(name = "total_failed", nullable = false)
    private int totalFailed = 0;

    @Column(name = "include_candidates", nullable = false)
    private boolean includeCandidates = false;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "convocation_audience",
            joinColumns = @JoinColumn(name = "convocation_id"),
            indexes = @Index(name = "idx_convocation_audience_conv", columnList = "convocation_id"))
    @Column(name = "audience_member_id", nullable = false)
    private Set<UUID> audienceMemberIds = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "convocation_channels",
            joinColumns = @JoinColumn(name = "convocation_id"),
            indexes = @Index(name = "idx_convocation_channels_conv", columnList = "convocation_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    private Set<ConvocationChannel> channels = EnumSet.noneOf(ConvocationChannel.class);

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "convocation_reminders",
            joinColumns = @JoinColumn(name = "convocation_id"),
            indexes = @Index(name = "idx_convocation_reminders_conv", columnList = "convocation_id"))
    private List<ReminderEmbeddable> reminders = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @Version
    private Long version;
}
