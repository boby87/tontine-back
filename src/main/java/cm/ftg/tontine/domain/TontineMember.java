package cm.ftg.tontine.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entité de liaison entre un {@link User} et une {@link Tontine}.
 * CDC Section 7.1 — entité TontineMember.
 * <p>
 * Clé primaire : UUID unique (copilot-instructions §Sécurité).
 * Contrainte d'unicité : un utilisateur ne peut être membre qu'une seule fois par tontine.
 */
@Entity
@Table(
    name = "tontine_member",
    uniqueConstraints = @UniqueConstraint(columnNames = {"tontine_id", "user_id"})
)
public class TontineMember extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tontine_id", nullable = false)
    private Tontine tontine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Rôle au sein de cette tontine (MEMBRE par défaut) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TontineRole role = TontineRole.MEMBRE;

    /** Statut de l'appartenance (EN_ATTENTE jusqu'à approbation du bureau) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TontineMemberStatus status = TontineMemberStatus.EN_ATTENTE;

    @Column(name = "joined_at")
    private LocalDateTime joinedAt;

    @Column(name = "left_at")
    private LocalDateTime leftAt;

    /** Position dans l'ordre de rotation pour la distribution de la cagnotte */
    @Column(name = "rotation_order")
    private Integer rotationOrder;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Tontine getTontine() { return tontine; }
    public void setTontine(Tontine tontine) { this.tontine = tontine; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public TontineRole getRole() { return role; }
    public void setRole(TontineRole role) { this.role = role; }
    public TontineMemberStatus getStatus() { return status; }
    public void setStatus(TontineMemberStatus status) { this.status = status; }
    public LocalDateTime getJoinedAt() { return joinedAt; }
    public void setJoinedAt(LocalDateTime joinedAt) { this.joinedAt = joinedAt; }
    public LocalDateTime getLeftAt() { return leftAt; }
    public void setLeftAt(LocalDateTime leftAt) { this.leftAt = leftAt; }
    public Integer getRotationOrder() { return rotationOrder; }
    public void setRotationOrder(Integer rotationOrder) { this.rotationOrder = rotationOrder; }
}
