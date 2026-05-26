package cm.ftg.tontine.treasurer.mobilemoney.entity;

import cm.ftg.tontine.treasurer.cashbox.enums.MovementDirection;
import cm.ftg.tontine.treasurer.mobilemoney.enums.MobileMoneyProvider;
import cm.ftg.tontine.treasurer.mobilemoney.enums.MobileMoneyStatus;
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
@Table(name = "mobile_money_transactions", indexes = {
        @Index(name = "idx_momo_tontine", columnList = "tontine_id"),
        @Index(name = "idx_momo_status", columnList = "status"),
        @Index(name = "idx_momo_received", columnList = "received_at")
})
@Getter
@Setter
@NoArgsConstructor
public class MobileMoneyTransaction {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tontine_id", nullable = false)
    private UUID tontineId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MobileMoneyProvider provider;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 5)
    private MovementDirection direction;

    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "from_phone", length = 30)
    private String fromPhone;

    @Column(name = "to_phone", length = 30)
    private String toPhone;

    @Column(name = "external_reference", length = 120, nullable = false)
    private String externalReference;

    @Column(name = "matched_member_id")
    private UUID matchedMemberId;

    @Column(name = "matched_member_full_name", length = 160)
    private String matchedMemberFullName;

    @Column(name = "contribution_id")
    private UUID contributionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MobileMoneyStatus status = MobileMoneyStatus.PENDING_APPROVAL;

    @CreationTimestamp
    @Column(name = "received_at", updatable = false, nullable = false)
    private Instant receivedAt;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "reviewed_by_full_name", length = 160)
    private String reviewedByFullName;

    @Column(name = "rejection_reason", length = 1000)
    private String rejectionReason;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;
}
