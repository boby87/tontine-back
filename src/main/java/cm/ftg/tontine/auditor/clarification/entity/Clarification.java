package cm.ftg.tontine.auditor.clarification.entity;

import cm.ftg.tontine.auditor.clarification.enums.ClarificationEvaluation;
import cm.ftg.tontine.auditor.clarification.enums.ClarificationStatus;
import cm.ftg.tontine.common.enums.UserRole;
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
@Table(name = "clarifications", indexes = {
        @Index(name = "idx_clarification_tontine", columnList = "tontine_id"),
        @Index(name = "idx_clarification_asked_at", columnList = "asked_at")
})
@Getter
@Setter
@NoArgsConstructor
public class Clarification {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tontine_id", nullable = false)
    private UUID tontineId;

    @Column(nullable = false, length = 160)
    private String subject;

    @Column(nullable = false, length = 4000)
    private String question;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_role", nullable = false, length = 20)
    private UserRole targetRole;

    @Column(name = "due_within_hours", nullable = false)
    private int dueWithinHours;

    @Column(name = "asked_by_user_id", nullable = false)
    private UUID askedByUserId;

    @Column(name = "asked_by_full_name", nullable = false, length = 160)
    private String askedByFullName;

    @CreationTimestamp
    @Column(name = "asked_at", updatable = false, nullable = false)
    private Instant askedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ClarificationStatus status = ClarificationStatus.PENDING;

    @Column(length = 4000)
    private String response;

    @Column(name = "responded_at")
    private Instant respondedAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ClarificationEvaluation evaluation;

    @Column(name = "evaluated_at")
    private Instant evaluatedAt;

    @Version
    private Long version;
}
