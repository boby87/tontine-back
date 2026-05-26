package cm.ftg.tontine.treasurer.cashbox.entity;

import cm.ftg.tontine.treasurer.cashbox.enums.CashTransferStatus;
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

@Entity
@Table(name = "cash_box_transfers", indexes = {
        @Index(name = "idx_transfer_tontine", columnList = "tontine_id"),
        @Index(name = "idx_transfer_status", columnList = "status"),
        @Index(name = "idx_transfer_requested_at", columnList = "requested_at")
})
@Getter
@Setter
@NoArgsConstructor
public class CashBoxTransfer {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tontine_id", nullable = false)
    private UUID tontineId;

    @Column(name = "from_cash_box_id", nullable = false)
    private UUID fromCashBoxId;

    @Column(name = "from_cash_box_name", nullable = false, length = 120)
    private String fromCashBoxName;

    @Column(name = "to_cash_box_id", nullable = false)
    private UUID toCashBoxId;

    @Column(name = "to_cash_box_name", nullable = false, length = 120)
    private String toCashBoxName;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 1000)
    private String justification;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CashTransferStatus status = CashTransferStatus.DRAFT;

    @Column(name = "president_approved_at")
    private Instant presidentApprovedAt;

    @Column(name = "auditor_approved_at")
    private Instant auditorApprovedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "rejected_at")
    private Instant rejectedAt;

    @Column(name = "rejection_reason", length = 1000)
    private String rejectionReason;

    @Column(name = "requested_by_full_name", nullable = false, length = 160)
    private String requestedByFullName;

    @CreationTimestamp
    @Column(name = "requested_at", updatable = false, nullable = false)
    private Instant requestedAt;

    @Version
    private Long version;
}
