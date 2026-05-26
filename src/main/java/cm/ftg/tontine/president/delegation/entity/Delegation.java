package cm.ftg.tontine.president.delegation.entity;

import cm.ftg.tontine.common.enums.UserRole;
import cm.ftg.tontine.president.delegation.enums.DelegationPower;
import cm.ftg.tontine.president.delegation.enums.DelegationStatus;
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
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "delegations", indexes = {
        @Index(name = "idx_delegation_tontine", columnList = "tontine_id"),
        @Index(name = "idx_delegation_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
public class Delegation {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tontine_id", nullable = false)
    private UUID tontineId;

    @Column(name = "delegatee_user_id", nullable = false)
    private UUID delegateeUserId;

    @Column(name = "delegatee_full_name", nullable = false, length = 160)
    private String delegateeFullName;

    @Enumerated(EnumType.STRING)
    @Column(name = "delegatee_role", nullable = false, length = 20)
    private UserRole delegateeRole;

    @ElementCollection(fetch = FetchType.EAGER, targetClass = DelegationPower.class)
    @CollectionTable(name = "delegation_powers",
            joinColumns = @JoinColumn(name = "delegation_id"),
            indexes = @Index(name = "idx_delegation_power_did", columnList = "delegation_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "power", nullable = false, length = 30)
    private Set<DelegationPower> powers = EnumSet.noneOf(DelegationPower.class);

    @Column(nullable = false, length = 1000)
    private String reason;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DelegationStatus status = DelegationStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "revoked_reason", length = 1000)
    private String revokedReason;

    @Version
    private Long version;
}
