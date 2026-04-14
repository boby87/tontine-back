package cm.ftg.tontine.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * Sanction appliquée à un membre lors d'une séance.
 * CDC Section 4.3 — Module Gestion des Sanctions.
 * <p>
 * Clé primaire : UUID unique (copilot-instructions §Sécurité).
 * Clé d'idempotence : empêche la double-sanction du même type pour le même membre dans la même séance.
 * Soft delete interdit — les sanctions sont immutables (audit).
 */
@Entity
@Table(
    name = "sanction",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_sanction_idempotence",
        columnNames = {"cle_idempotence"}
    )
)
public class Sanction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /** Clé d'idempotence : SAN-{memberId}-{sessionId}-{type} */
    @Column(name = "cle_idempotence", unique = true, nullable = false)
    private String cleIdempotence;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private TontineMember member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SanctionType type;

    @Column(precision = 15, scale = 2, nullable = false)
    private BigDecimal montant;

    @Column(length = 500)
    private String motif;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SanctionStatus status = SanctionStatus.IMPAYEE;

    /** ID du censeur ayant appliqué la sanction (pour audit) */
    @Column(name = "applied_by_member_id", nullable = false)
    private String appliedByMemberId;

    @Version
    private Long version;

    // ── Getters / Setters ──

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCleIdempotence() { return cleIdempotence; }
    public void setCleIdempotence(String cleIdempotence) { this.cleIdempotence = cleIdempotence; }
    public TontineMember getMember() { return member; }
    public void setMember(TontineMember member) { this.member = member; }
    public Session getSession() { return session; }
    public void setSession(Session session) { this.session = session; }
    public SanctionType getType() { return type; }
    public void setType(SanctionType type) { this.type = type; }
    public BigDecimal getMontant() { return montant; }
    public void setMontant(BigDecimal montant) { this.montant = montant; }
    public String getMotif() { return motif; }
    public void setMotif(String motif) { this.motif = motif; }
    public SanctionStatus getStatus() { return status; }
    public void setStatus(SanctionStatus status) { this.status = status; }
    public String getAppliedByMemberId() { return appliedByMemberId; }
    public void setAppliedByMemberId(String appliedByMemberId) { this.appliedByMemberId = appliedByMemberId; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
