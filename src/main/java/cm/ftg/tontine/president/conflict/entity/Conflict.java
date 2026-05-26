package cm.ftg.tontine.president.conflict.entity;

import cm.ftg.tontine.president.conflict.enums.ConflictDecisionOutcome;
import cm.ftg.tontine.president.conflict.enums.ConflictSeverity;
import cm.ftg.tontine.president.conflict.enums.ConflictStatus;
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
import org.hibernate.annotations.UpdateTimestamp;

// TODO: history collection not yet modelled

@Entity
@Table(name = "conflicts", indexes = {
        @Index(name = "idx_conflict_tontine", columnList = "tontine_id"),
        @Index(name = "idx_conflict_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
public class Conflict {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tontine_id", nullable = false)
    private UUID tontineId;

    @Column(nullable = false, length = 200)
    private String subject;

    @Column(nullable = false, length = 4000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ConflictStatus status = ConflictStatus.OPEN;

    @Column(name = "escalated_by_user_id")
    private UUID escalatedByUserId;

    @Column(name = "escalated_by_full_name", length = 160)
    private String escalatedByFullName;

    @CreationTimestamp
    @Column(name = "escalated_at", updatable = false, nullable = false)
    private Instant escalatedAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ConflictSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision_outcome", length = 30)
    private ConflictDecisionOutcome decisionOutcome;

    @Column(name = "decision_comment", length = 2000)
    private String decisionComment;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "mediation_scheduled_at")
    private Instant mediationScheduledAt;

    @Column(name = "mediation_note", length = 2000)
    private String mediationNote;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;
}
