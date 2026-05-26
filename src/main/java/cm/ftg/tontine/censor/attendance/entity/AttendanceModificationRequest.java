package cm.ftg.tontine.censor.attendance.entity;

import cm.ftg.tontine.censor.attendance.enums.AttendanceModificationStatus;
import cm.ftg.tontine.president.session.enums.AttendanceStatus;
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
@Table(name = "attendance_modifications", indexes = {
        @Index(name = "idx_att_mod_tontine", columnList = "tontine_id"),
        @Index(name = "idx_att_mod_session", columnList = "session_id"),
        @Index(name = "idx_att_mod_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
public class AttendanceModificationRequest {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tontine_id", nullable = false)
    private UUID tontineId;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "session_number")
    private Integer sessionNumber;

    @Column(name = "member_id", nullable = false)
    private UUID memberId;

    @Column(name = "member_full_name", length = 160)
    private String memberFullName;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", nullable = false, length = 20)
    private AttendanceStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 20)
    private AttendanceStatus toStatus;

    @Column(length = 2000)
    private String reason;

    @Column(name = "requested_by_user_id", nullable = false)
    private UUID requestedByUserId;

    @Column(name = "requested_by_full_name", length = 160)
    private String requestedByFullName;

    @CreationTimestamp
    @Column(name = "requested_at", updatable = false, nullable = false)
    private Instant requestedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttendanceModificationStatus status = AttendanceModificationStatus.PENDING;

    @Column(name = "decision_comment", length = 2000)
    private String decisionComment;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "linked_sanction_id")
    private UUID linkedSanctionId;

    @Column(name = "info_request", length = 2000)
    private String infoRequest;

    @Column(name = "info_response", length = 2000)
    private String infoResponse;

    @Version
    private Long version;
}
