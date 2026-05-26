package cm.ftg.tontine.secretary.agenda.entity;

import cm.ftg.tontine.secretary.agenda.enums.AgendaDraftStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "agenda_drafts", indexes = {
        @Index(name = "idx_agenda_drafts_tontine", columnList = "tontine_id"),
        @Index(name = "idx_agenda_drafts_session", columnList = "session_id"),
        @Index(name = "idx_agenda_drafts_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
public class AgendaDraft {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tontine_id", nullable = false)
    private UUID tontineId;

    @Column(name = "session_id")
    private UUID sessionId;

    @Column(name = "session_number", nullable = false)
    private int sessionNumber;

    @Column(name = "scheduled_at", nullable = false)
    private Instant scheduledAt;

    @Column(length = 200)
    private String location;

    @Column(name = "beneficiary_member_id")
    private UUID beneficiaryMemberId;

    @Column(name = "beneficiary_full_name", length = 160)
    private String beneficiaryFullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AgendaDraftStatus status = AgendaDraftStatus.DRAFT;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "president_comment", length = 2000)
    private String presidentComment;

    @Version
    private Long version;
}
