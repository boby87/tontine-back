package cm.ftg.tontine.secretary.minutes.entity;

import cm.ftg.tontine.secretary.minutes.enums.MinutesStatus;
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
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

// TODO: attachments collection not yet modelled

@Entity
@Table(name = "minutes_drafts", indexes = {
        @Index(name = "idx_minutes_tontine", columnList = "tontine_id"),
        @Index(name = "idx_minutes_session", columnList = "session_id"),
        @Index(name = "idx_minutes_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
public class MinutesDraft {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tontine_id", nullable = false)
    private UUID tontineId;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "session_number", nullable = false)
    private int sessionNumber;

    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MinutesStatus status = MinutesStatus.DRAFT;

    @Column(name = "secretary_signed_at")
    private Instant secretarySignedAt;

    @Column(name = "president_signed_at")
    private Instant presidentSignedAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "president_comment", length = 2000)
    private String presidentComment;

    @Column(name = "att_present", nullable = false)
    private int attPresent = 0;

    @Column(name = "att_late", nullable = false)
    private int attLate = 0;

    @Column(name = "att_absent", nullable = false)
    private int attAbsent = 0;

    @Column(name = "att_excused", nullable = false)
    private int attExcused = 0;

    @Column(name = "att_total", nullable = false)
    private int attTotal = 0;

    @Column(name = "att_quorum_reached", nullable = false)
    private boolean attQuorumReached = false;

    @Column(name = "fin_total_collected", precision = 19, scale = 2, nullable = false)
    private BigDecimal finTotalCollected = BigDecimal.ZERO;

    @Column(name = "fin_total_distributed", precision = 19, scale = 2, nullable = false)
    private BigDecimal finTotalDistributed = BigDecimal.ZERO;

    @Column(name = "fin_beneficiary_full_name", length = 160)
    private String finBeneficiaryFullName;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;
}
