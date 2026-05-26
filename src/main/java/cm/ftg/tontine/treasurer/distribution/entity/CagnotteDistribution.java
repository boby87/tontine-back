package cm.ftg.tontine.treasurer.distribution.entity;

import cm.ftg.tontine.treasurer.common.enums.PaymentMethod;
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
@Table(name = "cagnotte_distributions", indexes = {
        @Index(name = "idx_distribution_tontine", columnList = "tontine_id"),
        @Index(name = "idx_distribution_session", columnList = "session_id"),
        @Index(name = "idx_distribution_paid_at", columnList = "treasurer_paid_at")
})
@Getter
@Setter
@NoArgsConstructor
public class CagnotteDistribution {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "session_number", nullable = false)
    private int sessionNumber;

    @Column(name = "tontine_id", nullable = false)
    private UUID tontineId;

    @Column(name = "beneficiary_member_id", nullable = false)
    private UUID beneficiaryMemberId;

    @Column(name = "beneficiary_full_name", nullable = false, length = 160)
    private String beneficiaryFullName;

    @Column(name = "beneficiary_phone", nullable = false, length = 20)
    private String beneficiaryPhone;

    @Column(name = "gross_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal grossAmount;

    @Column(name = "deduction_emergency", nullable = false, precision = 19, scale = 2)
    private BigDecimal deductionEmergency;

    @Column(name = "deduction_operations", nullable = false, precision = 19, scale = 2)
    private BigDecimal deductionOperations;

    @Column(name = "net_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal netAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 30)
    private PaymentMethod paymentMethod;

    @Column(name = "beneficiary_confirmed", nullable = false)
    private boolean beneficiaryConfirmed = false;

    @Column(name = "beneficiary_confirmed_at")
    private Instant beneficiaryConfirmedAt;

    @Column(name = "treasurer_paid_at")
    private Instant treasurerPaidAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @Version
    private Long version;
}
