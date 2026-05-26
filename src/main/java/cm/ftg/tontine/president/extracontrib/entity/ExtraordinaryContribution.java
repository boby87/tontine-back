package cm.ftg.tontine.president.extracontrib.entity;

import cm.ftg.tontine.president.extracontrib.enums.ExtraContributionStatus;
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

@Entity
@Table(name = "extra_contributions", indexes = {
        @Index(name = "idx_extra_tontine", columnList = "tontine_id"),
        @Index(name = "idx_extra_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
public class ExtraordinaryContribution {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tontine_id", nullable = false)
    private UUID tontineId;

    @Column(nullable = false, length = 500)
    private String motive;

    @Column(name = "beneficiary_member_id")
    private UUID beneficiaryMemberId;

    @Column(name = "beneficiary_full_name", length = 160)
    private String beneficiaryFullName;

    @Column(name = "amount_per_member", precision = 19, scale = 2, nullable = false)
    private BigDecimal amountPerMember;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExtraContributionStatus status = ExtraContributionStatus.DRAFT;

    @Column(name = "exempt_beneficiary", nullable = false)
    private boolean exemptBeneficiary = false;

    @Column(name = "total_expected", precision = 19, scale = 2, nullable = false)
    private BigDecimal totalExpected = BigDecimal.ZERO;

    @Column(name = "total_collected", precision = 19, scale = 2, nullable = false)
    private BigDecimal totalCollected = BigDecimal.ZERO;

    @Column(name = "voted_by_assembly_at")
    private Instant votedByAssemblyAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "distributed_at")
    private Instant distributedAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;
}
