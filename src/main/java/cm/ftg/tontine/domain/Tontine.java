package cm.ftg.tontine.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Groupe de tontine avec tous ses paramètres de configuration.
 * CDC Section 7.1 — entité Tontine.
 * Soft delete via deletedAt.
 */
@Entity
@Table(name = "tontine")
public class Tontine extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Identité ──────────────────────────────────────────────────────────────

    @Column(nullable = false)
    private String nom;

    @Column(length = 1000)
    private String description;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "primary_color")
    private String primaryColor;

    @Column(name = "secondary_color")
    private String secondaryColor;

    @Enumerated(EnumType.STRING)
    @Column(name = "theme", nullable = false)
    private TontineTheme theme = TontineTheme.LIGHT;

    @Column(nullable = false)
    private String currency = "XAF";

    // ── Cotisations & amendes ─────────────────────────────────────────────────

    @Column(name = "montant_cotisation", precision = 15, scale = 2, nullable = false)
    private BigDecimal montantCotisation;

    @Enumerated(EnumType.STRING)
    @Column(name = "contribution_frequency", nullable = false)
    private ContributionFrequency contributionFrequency = ContributionFrequency.MONTHLY;

    @Column(name = "taux_amende_forfaitaire_jour", precision = 5, scale = 2, nullable = false)
    private BigDecimal tauxAmendeForfaitaireJour;

    @Column(name = "plafond_amende_en_pourcentage", precision = 5, scale = 2, nullable = false)
    private BigDecimal plafondAmendeEnPourcentage;

    // ── Distribution ──────────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "distribution_mode", nullable = false)
    private DistributionMode distributionMode = DistributionMode.ROTATION;

    /** Nombre de séances par cycle (= nombre de membres bénéficiaires par cycle) */
    @Column(name = "cycle_sessions_count")
    private Integer cycleSessionsCount;

    // ── Prêts ─────────────────────────────────────────────────────────────────

    @Column(name = "loan_enabled", nullable = false)
    private boolean loanEnabled = false;

    @Column(name = "loan_interest_rate", precision = 5, scale = 2)
    private BigDecimal loanInterestRate;

    @Column(name = "loan_max_duration_months")
    private Integer loanMaxDurationMonths;

    @Column(name = "loan_max_amount", precision = 15, scale = 2)
    private BigDecimal loanMaxAmount;

    @Column(name = "loan_guarantors_required")
    private Integer loanGuarantorsRequired;

    // ── Bureau ────────────────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "bureau_designation_mode", nullable = false)
    private DesignationMode bureauDesignationMode = DesignationMode.NOMINATION;

    @Column(name = "bureau_mandate_duration_months")
    private Integer bureauMandateDurationMonths;

    // ── Audit & cycle de vie ──────────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private User createdBy;

    /** Soft delete — ne jamais supprimer physiquement une tontine */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }
    public String getPrimaryColor() { return primaryColor; }
    public void setPrimaryColor(String primaryColor) { this.primaryColor = primaryColor; }
    public String getSecondaryColor() { return secondaryColor; }
    public void setSecondaryColor(String secondaryColor) { this.secondaryColor = secondaryColor; }
    public TontineTheme getTheme() { return theme; }
    public void setTheme(TontineTheme theme) { this.theme = theme; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public BigDecimal getMontantCotisation() { return montantCotisation; }
    public void setMontantCotisation(BigDecimal montantCotisation) { this.montantCotisation = montantCotisation; }
    public ContributionFrequency getContributionFrequency() { return contributionFrequency; }
    public void setContributionFrequency(ContributionFrequency contributionFrequency) { this.contributionFrequency = contributionFrequency; }
    public BigDecimal getTauxAmendeForfaitaireJour() { return tauxAmendeForfaitaireJour; }
    public void setTauxAmendeForfaitaireJour(BigDecimal tauxAmendeForfaitaireJour) { this.tauxAmendeForfaitaireJour = tauxAmendeForfaitaireJour; }
    public BigDecimal getPlafondAmendeEnPourcentage() { return plafondAmendeEnPourcentage; }
    public void setPlafondAmendeEnPourcentage(BigDecimal plafondAmendeEnPourcentage) { this.plafondAmendeEnPourcentage = plafondAmendeEnPourcentage; }
    public DistributionMode getDistributionMode() { return distributionMode; }
    public void setDistributionMode(DistributionMode distributionMode) { this.distributionMode = distributionMode; }
    public Integer getCycleSessionsCount() { return cycleSessionsCount; }
    public void setCycleSessionsCount(Integer cycleSessionsCount) { this.cycleSessionsCount = cycleSessionsCount; }
    public boolean isLoanEnabled() { return loanEnabled; }
    public void setLoanEnabled(boolean loanEnabled) { this.loanEnabled = loanEnabled; }
    public BigDecimal getLoanInterestRate() { return loanInterestRate; }
    public void setLoanInterestRate(BigDecimal loanInterestRate) { this.loanInterestRate = loanInterestRate; }
    public Integer getLoanMaxDurationMonths() { return loanMaxDurationMonths; }
    public void setLoanMaxDurationMonths(Integer loanMaxDurationMonths) { this.loanMaxDurationMonths = loanMaxDurationMonths; }
    public BigDecimal getLoanMaxAmount() { return loanMaxAmount; }
    public void setLoanMaxAmount(BigDecimal loanMaxAmount) { this.loanMaxAmount = loanMaxAmount; }
    public Integer getLoanGuarantorsRequired() { return loanGuarantorsRequired; }
    public void setLoanGuarantorsRequired(Integer loanGuarantorsRequired) { this.loanGuarantorsRequired = loanGuarantorsRequired; }
    public DesignationMode getBureauDesignationMode() { return bureauDesignationMode; }
    public void setBureauDesignationMode(DesignationMode bureauDesignationMode) { this.bureauDesignationMode = bureauDesignationMode; }
    public Integer getBureauMandateDurationMonths() { return bureauMandateDurationMonths; }
    public void setBureauMandateDurationMonths(Integer bureauMandateDurationMonths) { this.bureauMandateDurationMonths = bureauMandateDurationMonths; }
    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }
}
