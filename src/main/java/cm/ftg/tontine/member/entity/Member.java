package cm.ftg.tontine.member.entity;

import cm.ftg.tontine.common.enums.MemberStatus;
import cm.ftg.tontine.common.enums.UserRole;
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
@Table(name = "members",
        uniqueConstraints = @UniqueConstraint(name = "uk_member_user_tontine",
                columnNames = {"user_id", "tontine_id"}),
        indexes = {
                @Index(name = "idx_members_user", columnList = "user_id"),
                @Index(name = "idx_members_tontine", columnList = "tontine_id")
        })
@Getter
@Setter
@NoArgsConstructor
public class Member {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "tontine_id", nullable = false)
    private UUID tontineId;

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

    @Column(length = 255)
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberStatus status = MemberStatus.PENDING;

    @ElementCollection(fetch = FetchType.EAGER, targetClass = UserRole.class)
    @CollectionTable(name = "member_roles", joinColumns = @JoinColumn(name = "member_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Set<UserRole> roles = EnumSet.noneOf(UserRole.class);

    @CreationTimestamp
    @Column(name = "joined_at", updatable = false, nullable = false)
    private Instant joinedAt;

    private Integer tourOrder;

    @Column(nullable = false)
    private boolean hasReceivedTour = false;

    @Column(name = "total_contributed", precision = 19, scale = 2, nullable = false)
    private BigDecimal totalContributed = BigDecimal.ZERO;

    @Column(name = "total_arrears", precision = 19, scale = 2, nullable = false)
    private BigDecimal totalArrears = BigDecimal.ZERO;
}
