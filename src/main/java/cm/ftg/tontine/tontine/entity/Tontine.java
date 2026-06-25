package cm.ftg.tontine.tontine.entity;

import cm.ftg.tontine.common.entity.BaseEntity;
import cm.ftg.tontine.common.enums.AuctionSacrificeDestination;
import cm.ftg.tontine.common.enums.BureauDesignationMode;
import cm.ftg.tontine.common.enums.ContributionFrequency;
import cm.ftg.tontine.common.enums.DistributionMode;
import cm.ftg.tontine.common.enums.TontineStatus;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tontines")
@Getter
@Setter
@NoArgsConstructor
public class Tontine extends BaseEntity {

    // ── Identité ────────────────────────────────────────────────────────────

    @Column(nullable = false, length = 160)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(name = "logo_url", length = 512)
    private String logoUrl;

    @Column(name = "primary_color", length = 10)
    private String primaryColor;

    @Column(name = "secondary_color", length = 10)
    private String secondaryColor;

    /** "LIGHT" ou "DARK". */
    @Column(length = 10)
    private String theme;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TontineStatus status = TontineStatus.DRAFT;

    // ── Cotisations ─────────────────────────────────────────────────────────

    @Column(name = "contribution_amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal contributionAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ContributionFrequency frequency;

    @Column(name = "partial_contribution_allowed", nullable = false)
    private boolean partialContributionAllowed = false;

    @Column(name = "partial_contribution_min", precision = 19, scale = 2)
    private BigDecimal partialContributionMin;

    /** Jours avant la séance pour la date limite de paiement. */
    @Column(name = "payment_deadline_days")
    private Integer paymentDeadlineDays;

    /** Montant au-delà duquel une double validation est requise pour les dépenses. */
    @Column(name = "expense_approval_threshold", precision = 19, scale = 2)
    private BigDecimal expenseApprovalThreshold;

    /** Frais d'adhésion (0 = gratuit). */
    @Column(name = "membership_fee", precision = 19, scale = 2, nullable = false)
    private BigDecimal membershipFee = BigDecimal.ZERO;

    // ── Cycle et distribution ────────────────────────────────────────────────

    @Column(name = "cycle_sessions_count")
    private Integer cycleSessionsCount;

    @Column(name = "auto_new_cycle", nullable = false)
    private boolean autoNewCycle = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "distribution_mode", nullable = false, length = 20)
    private DistributionMode distributionMode = DistributionMode.ROTATION;

    @Enumerated(EnumType.STRING)
    @Column(name = "auction_sacrifice_destination", length = 20)
    private AuctionSacrificeDestination auctionSacrificeDestination;

    // ── Prêts ───────────────────────────────────────────────────────────────

    @Column(name = "loan_enabled", nullable = false)
    private boolean loanEnabled = false;

    @Column(name = "loan_interest_rate", precision = 5, scale = 2)
    private BigDecimal loanInterestRate;

    @Column(name = "loan_max_duration_months")
    private Integer loanMaxDurationMonths;

    @Column(name = "loan_max_amount", precision = 19, scale = 2)
    private BigDecimal loanMaxAmount;

    @Column(name = "loan_guarantors_required")
    private Integer loanGuarantorsRequired;

    // ── Bureau ──────────────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "bureau_designation_mode", nullable = false, length = 20)
    private BureauDesignationMode bureauDesignationMode = BureauDesignationMode.NOMINATION;

    /** Durée des mandats en mois. null = illimité. */
    @Column(name = "bureau_mandate_duration_months")
    private Integer bureauMandateDurationMonths;

    // ── Sanctions ───────────────────────────────────────────────────────────

    /** Nombre max de sanctions avant proposition de suspension. */
    @Column(name = "sanction_exclusion_threshold")
    private Integer sanctionExclusionThreshold;

    // ── État général ─────────────────────────────────────────────────────────

    @Column(nullable = false)
    private int maxMembers;

    @Column(name = "member_count", nullable = false)
    private int memberCount = 0;

    @Column(name = "current_cycle_id")
    private UUID currentCycleId;

    @Column(name = "total_saved", precision = 19, scale = 2, nullable = false)
    private BigDecimal totalSaved = BigDecimal.ZERO;

    @Column(name = "created_by_user_id", nullable = false)
    private UUID createdByUserId;

    @Column(name = "is_emergency_blocked", nullable = false)
    private boolean isEmergencyBlocked = false;

    @Column(name = "dissolution_reason", length = 2000)
    private String dissolutionReason;

    @Column(nullable = false)
    private LocalDate startDate;

    private LocalDate endDate;

    @Embedded
    private TontineRulesEmbeddable rules;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "tontine_founders", joinColumns = @JoinColumn(name = "tontine_id"))
    private List<FounderInviteEmbeddable> founders = new ArrayList<>();

    @Version
    private Long version;
}
