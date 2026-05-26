package cm.ftg.tontine.secretary.rsvp.entity;

import cm.ftg.tontine.secretary.rsvp.enums.RsvpStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "session_rsvps",
        uniqueConstraints = @UniqueConstraint(name = "uk_rsvp_session_member",
                columnNames = {"session_id", "member_id"}),
        indexes = {
                @Index(name = "idx_rsvps_session", columnList = "session_id"),
                @Index(name = "idx_rsvps_member", columnList = "member_id")
        })
@Getter
@Setter
@NoArgsConstructor
public class SessionRsvp {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "member_id", nullable = false)
    private UUID memberId;

    @Column(name = "member_full_name", nullable = false, length = 160)
    private String memberFullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RsvpStatus status = RsvpStatus.PENDING;

    @Column(length = 2000)
    private String reason;

    @Column(name = "responded_at")
    private Instant respondedAt;

    @Version
    private Long version;
}
