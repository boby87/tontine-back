package cm.ftg.tontine.treasurer.loan.entity;

import cm.ftg.tontine.treasurer.loan.enums.LoanStatus;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "loans", indexes = {
        @Index(name = "idx_loan_tontine", columnList = "tontine_id"),
        @Index(name = "idx_loan_member", columnList = "member_id"),
        @Index(name = "idx_loan_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
public class Loan {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tontine_id", nullable = false)
    private UUID tontineId;

    @Column(name = "member_id", nullable = false)
    private UUID memberId;

    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal principal = BigDecimal.ZERO;

    @Column(name = "interest_rate", precision = 7, scale = 4, nullable = false)
    private BigDecimal interestRate = BigDecimal.ZERO;

    @Column(name = "duration_months", nullable = false)
    private int durationMonths;

    @Column(name = "monthly_payment", precision = 19, scale = 2, nullable = false)
    private BigDecimal monthlyPayment = BigDecimal.ZERO;

    @Column(name = "total_due", precision = 19, scale = 2, nullable = false)
    private BigDecimal totalDue = BigDecimal.ZERO;

    @Column(name = "total_repaid", precision = 19, scale = 2, nullable = false)
    private BigDecimal totalRepaid = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private LoanStatus status = LoanStatus.REQUESTED;

    @Column(length = 1000)
    private String purpose;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "loan_guarantors", joinColumns = @JoinColumn(name = "loan_id"))
    @Column(name = "guarantor_id", nullable = false)
    private List<UUID> guarantorIds = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "requested_at", updatable = false, nullable = false)
    private Instant requestedAt;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "disbursed_at")
    private Instant disbursedAt;

    @Column(name = "due_date")
    private Instant dueDate;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;
}
