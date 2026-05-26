package cm.ftg.tontine.president.sanction.entity;

import cm.ftg.tontine.president.sanction.enums.SanctionCancelByRole;
import cm.ftg.tontine.president.sanction.enums.SanctionSeverity;
import cm.ftg.tontine.president.sanction.enums.SanctionStatus;
import cm.ftg.tontine.president.sanction.enums.SanctionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "sanctions", indexes = {
        @Index(name = "idx_sanction_tontine", columnList = "tontine_id"),
        @Index(name = "idx_sanction_member", columnList = "member_id"),
        @Index(name = "idx_sanction_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
public class Sanction {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tontine_id", nullable = false)
    private UUID tontineId;

    @Column(name = "member_id", nullable = false)
    private UUID memberId;

    @Column(name = "member_full_name", length = 160)
    private String memberFullName;

    @Column(name = "session_id")
    private UUID sessionId;

    @Column(name = "session_number")
    private Integer sessionNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SanctionType type;

    @Column(name = "custom_label", length = 160)
    private String customLabel;

    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "is_financial", nullable = false)
    private boolean financial = true;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private SanctionSeverity severity;

    @Column(length = 1000, nullable = false)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SanctionStatus status = SanctionStatus.PENDING;

    @Column(name = "auto_detected", nullable = false)
    private boolean autoDetected = false;

    @Column(name = "issued_by_user_id", nullable = false)
    private UUID issuedByUserId;

    @Column(name = "issued_by_full_name", length = 160)
    private String issuedByFullName;

    @CreationTimestamp
    @Column(name = "issued_at", updatable = false, nullable = false)
    private Instant issuedAt;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "contested_at")
    private Instant contestedAt;

    @Column(name = "contest_reason", length = 2000)
    private String contestReason;

    @Column(name = "contest_attachment_name", length = 255)
    private String contestAttachmentName;

    @Column(name = "contest_file_id")
    private UUID contestFileId;

    @Column(name = "resolved_by_user_id")
    private UUID resolvedByUserId;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancelled_by_user_id")
    private UUID cancelledByUserId;

    @Column(name = "cancelled_by_full_name", length = 160)
    private String cancelledByFullName;

    @Column(name = "cancel_reason", length = 1000)
    private String cancelReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "cancelled_by_role", length = 20)
    private SanctionCancelByRole cancelledByRole;

    @Column(name = "refund_initiated", nullable = false)
    private boolean refundInitiated = false;

    @Version
    private Long version;
}
