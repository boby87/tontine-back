package cm.ftg.tontine.member.entity;

import cm.ftg.tontine.common.entity.BaseEntity;
import cm.ftg.tontine.common.enums.MemberStatus;
import cm.ftg.tontine.common.enums.UserRole;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "tontine_members",
        uniqueConstraints = @UniqueConstraint(name = "uk_member_user_tontine",
                columnNames = {"user_id", "tontine_id"}),
        indexes = {
                @Index(name = "idx_members_user", columnList = "user_id"),
                @Index(name = "idx_members_tontine", columnList = "tontine_id")
        })
@Getter
@Setter
@NoArgsConstructor
public class Member extends BaseEntity {

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "tontine_id", nullable = false)
    private UUID tontineId;

    /** Rôles au sein de cette tontine — source de vérité pour les permissions métier. */
    @ElementCollection(fetch = FetchType.EAGER, targetClass = UserRole.class)
    @CollectionTable(name = "member_roles", joinColumns = @JoinColumn(name = "member_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 30)
    private Set<UserRole> roles = EnumSet.noneOf(UserRole.class);

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberStatus status = MemberStatus.PENDING;

    @Column(nullable = false, length = 40)
    private String matricule;

    @Column(nullable = false, length = 80)
    private String firstName;

    @Column(nullable = false, length = 80)
    private String lastName;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(length = 160)
    private String email;

    @Column(length = 512)
    private String avatarUrl;

    @Column(name = "kyc_photo_file_id")
    private UUID kycPhotoFileId;

    @CreationTimestamp
    @Column(name = "joined_at", updatable = false, nullable = false)
    private Instant joinedAt;

    /** Renseigné quand status passe à LEFT. */
    @Column(name = "left_at")
    private Instant leftAt;

    @Column(name = "suspended_at")
    private Instant suspendedAt;

    @Column(name = "suspension_reason", length = 1000)
    private String suspensionReason;

    /** Date depuis laquelle ce membre occupe le poste de Président (null si non Président). */
    @Column(name = "president_since")
    private Instant presidentSince;

    /** Expiration du mandat bureau (null = illimité). */
    @Column(name = "mandate_expires_at")
    private Instant mandateExpiresAt;

    @Column(name = "rotation_order")
    private Integer rotationOrder;

    @Column(name = "has_received_tour", nullable = false)
    private boolean hasReceivedTour = false;

    @Column(name = "total_contributed", precision = 19, scale = 2, nullable = false)
    private BigDecimal totalContributed = BigDecimal.ZERO;

    @Column(name = "total_arrears", precision = 19, scale = 2, nullable = false)
    private BigDecimal totalArrears = BigDecimal.ZERO;
}
