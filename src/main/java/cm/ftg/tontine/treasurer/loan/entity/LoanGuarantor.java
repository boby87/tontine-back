package cm.ftg.tontine.treasurer.loan.entity;

import cm.ftg.tontine.treasurer.loan.enums.GuarantorStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "loan_guarantors", indexes = {
        @Index(name = "idx_guarantor_loan", columnList = "loan_id"),
        @Index(name = "idx_guarantor_member", columnList = "guarantor_id")
})
@Getter
@Setter
@NoArgsConstructor
public class LoanGuarantor {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "loan_id", nullable = false)
    private UUID loanId;

    @Column(name = "guarantor_id", nullable = false)
    private UUID guarantorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GuarantorStatus status = GuarantorStatus.PENDING;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @Column(name = "responded_at")
    private Instant respondedAt;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;
}
