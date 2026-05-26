package cm.ftg.tontine.censor.justification.entity;

import cm.ftg.tontine.censor.justification.enums.JustificationStatus;
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
@Table(name = "absence_justifications", indexes = {
        @Index(name = "idx_just_tontine", columnList = "tontine_id"),
        @Index(name = "idx_just_session", columnList = "session_id"),
        @Index(name = "idx_just_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
public class AbsenceJustification {

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

    @Column(name = "document_name", length = 255)
    private String documentName;

    @Column(name = "document_type", length = 100)
    private String documentType;

    @Column(name = "document_size_kb")
    private Integer documentSizeKb;

    @Column(length = 2000)
    private String reason;

    @CreationTimestamp
    @Column(name = "submitted_at", updatable = false, nullable = false)
    private Instant submittedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private JustificationStatus status = JustificationStatus.PENDING_CENSOR;

    @Column(name = "censor_comment", length = 2000)
    private String censorComment;

    @Column(name = "censor_decided_at")
    private Instant censorDecidedAt;

    @Column(name = "president_comment", length = 2000)
    private String presidentComment;

    @Column(name = "president_decided_at")
    private Instant presidentDecidedAt;

    @Column(name = "linked_sanction_id")
    private UUID linkedSanctionId;

    @Version
    private Long version;
}
