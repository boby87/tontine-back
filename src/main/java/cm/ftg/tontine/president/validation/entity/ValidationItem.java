package cm.ftg.tontine.president.validation.entity;

import cm.ftg.tontine.common.enums.Priority;
import cm.ftg.tontine.president.validation.enums.AuditorOpinionStatus;
import cm.ftg.tontine.president.validation.enums.DecisionType;
import cm.ftg.tontine.president.validation.enums.DocumentKind;
import cm.ftg.tontine.president.validation.enums.FinancialOperationType;
import cm.ftg.tontine.president.validation.enums.ValidationCategory;
import cm.ftg.tontine.president.validation.enums.ValidationStatus;
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
@Table(name = "validations", indexes = {
        @Index(name = "idx_validation_tontine", columnList = "tontine_id"),
        @Index(name = "idx_validation_category_status", columnList = "category, status")
})
@Getter
@Setter
@NoArgsConstructor
public class ValidationItem {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tontine_id", nullable = false)
    private UUID tontineId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ValidationCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ValidationStatus status = ValidationStatus.PENDING;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Priority priority = Priority.NORMAL;

    @Column(name = "submitted_by_user_id", nullable = false)
    private UUID submittedByUserId;

    @Column(name = "submitted_by_full_name", nullable = false, length = 160)
    private String submittedByFullName;

    // Financial operation fields
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private FinancialOperationType operationType;

    @Column(length = 100)
    private String reference;

    @Column(name = "borrower_member_id")
    private UUID borrowerMemberId;

    @Column(name = "borrower_full_name", length = 160)
    private String borrowerFullName;

    @Column(name = "guarantors_json", length = 4000)
    private String guarantorsJson;

    @Column(name = "cash_box_name", length = 80)
    private String cashBoxName;

    @Column(name = "cash_box_balance_before", precision = 19, scale = 2)
    private BigDecimal cashBoxBalanceBefore;

    @Column(name = "cash_box_balance_after", precision = 19, scale = 2)
    private BigDecimal cashBoxBalanceAfter;

    @Column(name = "duration_months")
    private Integer durationMonths;

    @Column(name = "interest_rate", precision = 7, scale = 2)
    private BigDecimal interestRate;

    @Column(name = "total_due", precision = 19, scale = 2)
    private BigDecimal totalDue;

    // Document fields
    @Enumerated(EnumType.STRING)
    @Column(name = "document_kind", length = 30)
    private DocumentKind documentKind;

    @Column(name = "session_number")
    private Integer sessionNumber;

    @Column(name = "session_date")
    private LocalDate sessionDate;

    @Column(length = 200)
    private String location;

    @Column(length = 160)
    private String beneficiary;

    @Column(name = "agenda_points_json", length = 4000)
    private String agendaPointsJson;

    @Column(name = "signed_by_secretary")
    private Boolean signedBySecretary;

    @Column(name = "signed_by_secretary_at")
    private Instant signedBySecretaryAt;

    @Column(name = "attachments_json", length = 4000)
    private String attachmentsJson;

    @Column(name = "preview_snippet", length = 4000)
    private String previewSnippet;

    // Adhesion fields
    @Column(name = "candidate_full_name", length = 160)
    private String candidateFullName;

    @Column(name = "candidate_phone", length = 20)
    private String candidatePhone;

    @Column(name = "candidate_email", length = 160)
    private String candidateEmail;

    @Column(name = "sponsor_full_name", length = 160)
    private String sponsorFullName;

    @Column(name = "voted_by_assembly")
    private Boolean votedByAssembly;

    @Column(name = "vote_result", length = 20)
    private String voteResult;

    // Auditor opinion (embedded fields)
    @Enumerated(EnumType.STRING)
    @Column(name = "auditor_opinion_status", length = 20)
    private AuditorOpinionStatus auditorOpinionStatus;

    @Column(name = "auditor_opinion_comment", length = 2000)
    private String auditorOpinionComment;

    @Column(name = "auditor_user_id")
    private UUID auditorUserId;

    @Column(name = "auditor_full_name", length = 160)
    private String auditorFullName;

    @Column(name = "auditor_emitted_at")
    private Instant auditorEmittedAt;

    // Decision
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private DecisionType decision;

    @Column(name = "decision_comment", length = 2000)
    private String decisionComment;

    @Column(name = "decided_by_user_id")
    private UUID decidedByUserId;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @CreationTimestamp
    @Column(name = "submitted_at", updatable = false, nullable = false)
    private Instant submittedAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;
}
