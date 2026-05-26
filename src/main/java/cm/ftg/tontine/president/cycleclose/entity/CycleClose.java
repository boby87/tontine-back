package cm.ftg.tontine.president.cycleclose.entity;

import cm.ftg.tontine.president.cycleclose.enums.CycleCloseStatus;
import cm.ftg.tontine.president.cycleclose.enums.NextCycleDrawMode;
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
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "cycle_closes",
        uniqueConstraints = @UniqueConstraint(name = "uk_cycle_close_cycle", columnNames = {"cycle_id"}),
        indexes = {
                @Index(name = "idx_cycle_close_tontine", columnList = "tontine_id"),
                @Index(name = "idx_cycle_close_status", columnList = "status")
        })
@Getter
@Setter
@NoArgsConstructor
public class CycleClose {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tontine_id", nullable = false)
    private UUID tontineId;

    @Column(name = "cycle_id", nullable = false)
    private UUID cycleId;

    @Column(name = "cycle_number", nullable = false)
    private int cycleNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CycleCloseStatus status = CycleCloseStatus.NOT_STARTED;

    @Column(name = "total_collected", precision = 19, scale = 2, nullable = false)
    private BigDecimal totalCollected = BigDecimal.ZERO;

    @Column(name = "total_distributed", precision = 19, scale = 2, nullable = false)
    private BigDecimal totalDistributed = BigDecimal.ZERO;

    @Column(name = "total_loans_outstanding", precision = 19, scale = 2, nullable = false)
    private BigDecimal totalLoansOutstanding = BigDecimal.ZERO;

    @Column(name = "total_sanctions_collected", precision = 19, scale = 2, nullable = false)
    private BigDecimal totalSanctionsCollected = BigDecimal.ZERO;

    @Column(name = "net_result", precision = 19, scale = 2, nullable = false)
    private BigDecimal netResult = BigDecimal.ZERO;

    @Column(name = "members_retained", nullable = false)
    private int membersRetained = 0;

    @Column(name = "new_members_next_cycle", nullable = false)
    private int newMembersNextCycle = 0;

    @Column(name = "auditor_validated_at")
    private Instant auditorValidatedAt;

    @Column(name = "president_signed_at")
    private Instant presidentSignedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "next_cycle_start_date")
    private LocalDate nextCycleStartDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "next_cycle_draw_mode", length = 30)
    private NextCycleDrawMode nextCycleDrawMode;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;
}
