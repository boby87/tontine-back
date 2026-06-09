package cm.ftg.tontine.president.presidencytransfer.entity;

import cm.ftg.tontine.president.presidencytransfer.enums.PresidencyTransferStatus;
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
@Table(
        name = "presidency_transfers",
        indexes = {
                @Index(name = "idx_pt_tontine", columnList = "tontine_id"),
                @Index(name = "idx_pt_status", columnList = "status"),
                @Index(name = "idx_pt_target", columnList = "target_member_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class PresidencyTransfer {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tontine_id", nullable = false)
    private UUID tontineId;

    @Column(name = "initiated_by_user_id", nullable = false)
    private UUID initiatedByUserId;

    @Column(name = "initiated_by_full_name", nullable = false, length = 160)
    private String initiatedByFullName;

    @Column(name = "target_member_id", nullable = false)
    private UUID targetMemberId;

    @Column(name = "target_user_id")
    private UUID targetUserId;

    @Column(name = "target_member_full_name", nullable = false, length = 160)
    private String targetMemberFullName;

    @Column(nullable = false, length = 2000)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PresidencyTransferStatus status = PresidencyTransferStatus.PENDING;

    @CreationTimestamp
    @Column(name = "initiated_at", updatable = false, nullable = false)
    private Instant initiatedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "declined_at")
    private Instant declinedAt;

    @Column(name = "decline_reason", length = 1000)
    private String declineReason;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancelled_by_user_id")
    private UUID cancelledByUserId;

    @Column(name = "cancel_reason", length = 1000)
    private String cancelReason;

    @Version
    private Long version;
}
