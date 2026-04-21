package cm.ftg.tontine.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Invitation à rejoindre une tontine via un token unique.
 * <p>
 * <b>Sécurité :</b> Seul le hash SHA-256 du token est stocké en base.
 * Le token en clair n'est jamais persisté — un vol de base de données
 * ne permet pas de reconstituer les liens d'invitation.
 * <p>
 * CDC Section 7 — Soft delete non applicable (les invitations expirées
 * sont nettoyées périodiquement par un Virtual Thread programmé).
 */
@Entity
@Table(name = "tontine_invitation")
public class TontineInvitation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tontine_id", nullable = false)
    private Tontine tontine;

    /**
     * Hash SHA-256 du token (hexadécimal, 64 caractères).
     * Jamais le token en clair.
     */
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;

    @Column(name = "max_uses", nullable = false)
    private int maxUses = 1;

    @Column(name = "current_uses", nullable = false)
    private int currentUses = 0;

    // ── Méthodes métier ──────────────────────────────────────────────

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean hasRemainingUses() {
        return currentUses < maxUses;
    }

    public boolean isUsable() {
        return !isExpired() && hasRemainingUses();
    }

    public void incrementUses() {
        if (!isUsable()) {
            throw new IllegalStateException(
                "Invitation non utilisable (expirée=%s, uses=%d/%d)"
                    .formatted(isExpired(), currentUses, maxUses));
        }
        this.currentUses++;
    }

    // ── Getters / Setters ─────────────────────────────────────────────

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Tontine getTontine() { return tontine; }
    public void setTontine(Tontine tontine) { this.tontine = tontine; }
    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }
    public int getMaxUses() { return maxUses; }
    public void setMaxUses(int maxUses) { this.maxUses = maxUses; }
    public int getCurrentUses() { return currentUses; }
    public void setCurrentUses(int currentUses) { this.currentUses = currentUses; }
}

