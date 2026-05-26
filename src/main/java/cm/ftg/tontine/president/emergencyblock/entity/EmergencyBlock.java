package cm.ftg.tontine.president.emergencyblock.entity;

import cm.ftg.tontine.president.emergencyblock.enums.EmergencyBlockStatus;
import cm.ftg.tontine.president.emergencyblock.enums.EmergencyBlockTarget;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "emergency_blocks", indexes = {
        @Index(name = "idx_emergency_block_tontine", columnList = "tontine_id"),
        @Index(name = "idx_emergency_block_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
public class EmergencyBlock {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tontine_id", nullable = false)
    private UUID tontineId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EmergencyBlockTarget target;

    @Column(name = "target_ref", length = 160)
    private String targetRef;

    @Column(nullable = false, length = 1000)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EmergencyBlockStatus status = EmergencyBlockStatus.ACTIVE;

    @Column(name = "activated_by_user_id", nullable = false)
    private UUID activatedByUserId;

    @Column(name = "activated_by_full_name", nullable = false, length = 160)
    private String activatedByFullName;

    @CreationTimestamp
    @Column(name = "activated_at", updatable = false, nullable = false)
    private Instant activatedAt;

    @Column(name = "lifted_by_user_id")
    private UUID liftedByUserId;

    @Column(name = "lifted_at")
    private Instant liftedAt;

    @Column(name = "lift_reason", length = 1000)
    private String liftReason;

    @Version
    private Long version;
}
