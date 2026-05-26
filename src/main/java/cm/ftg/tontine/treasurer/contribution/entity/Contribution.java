package cm.ftg.tontine.treasurer.contribution.entity;

import cm.ftg.tontine.treasurer.common.enums.PaymentMethod;
import cm.ftg.tontine.treasurer.contribution.enums.ContributionStatus;
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
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "contributions",
        uniqueConstraints = @UniqueConstraint(name = "uk_contribution_session_member",
                columnNames = {"session_id", "member_id"}),
        indexes = {
                @Index(name = "idx_contribution_tontine", columnList = "tontine_id"),
                @Index(name = "idx_contribution_session", columnList = "session_id"),
                @Index(name = "idx_contribution_member", columnList = "member_id"),
                @Index(name = "idx_contribution_status", columnList = "status")
        })
@Getter
@Setter
@NoArgsConstructor
public class Contribution {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tontine_id", nullable = false)
    private UUID tontineId;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "member_id", nullable = false)
    private UUID memberId;

    @Column(name = "expected_amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal expectedAmount = BigDecimal.ZERO;

    @Column(name = "paid_amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ContributionStatus status = ContributionStatus.PENDING;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 20)
    private PaymentMethod paymentMethod;

    @Column(length = 120)
    private String reference;

    @Column(name = "collected_by_user_id")
    private UUID collectedByUserId;

    @Column(length = 1000)
    private String note;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;
}
