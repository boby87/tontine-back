package cm.ftg.tontine.president.session.entity;

import cm.ftg.tontine.president.session.enums.SessionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "sessions", indexes = {
        @Index(name = "idx_sessions_tontine", columnList = "tontine_id"),
        @Index(name = "idx_sessions_cycle", columnList = "cycle_id"),
        @Index(name = "idx_sessions_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
public class Session {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tontine_id", nullable = false)
    private UUID tontineId;

    @Column(name = "cycle_id", nullable = false)
    private UUID cycleId;

    @Column(nullable = false)
    private int number;

    @Column(name = "scheduled_at", nullable = false)
    private Instant scheduledAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(length = 200)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SessionStatus status = SessionStatus.SCHEDULED;

    @Column(name = "total_collected", precision = 19, scale = 2, nullable = false)
    private BigDecimal totalCollected = BigDecimal.ZERO;

    @Column(name = "total_distributed", precision = 19, scale = 2, nullable = false)
    private BigDecimal totalDistributed = BigDecimal.ZERO;

    @Column(name = "quorum_threshold", precision = 5, scale = 2, nullable = false)
    private BigDecimal quorumThreshold = BigDecimal.ZERO;

    @Column(name = "beneficiary_member_id")
    private UUID beneficiaryMemberId;

    @Column(name = "beneficiary_full_name", length = 160)
    private String beneficiaryFullName;

    @Column(name = "cagnotte_amount", precision = 19, scale = 2)
    private BigDecimal cagnotteAmount;

    @Column(name = "cagnotte_signed_by_president", nullable = false)
    private boolean cagnotteSignedByPresident = false;

    @Column(name = "next_session_date")
    private Instant nextSessionDate;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;
}
