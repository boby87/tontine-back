package cm.ftg.tontine.auditor.control.entity;

import cm.ftg.tontine.auditor.control.enums.ControlKind;
import cm.ftg.tontine.auditor.control.enums.ControlStatus;
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
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "controls", indexes = {
        @Index(name = "idx_control_tontine", columnList = "tontine_id"),
        @Index(name = "idx_control_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
public class Control {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tontine_id", nullable = false)
    private UUID tontineId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ControlKind kind;

    @Column(name = "period_from", nullable = false)
    private LocalDate periodFrom;

    @Column(name = "period_to", nullable = false)
    private LocalDate periodTo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ControlStatus status = ControlStatus.PLANNED;

    @Column(length = 4000)
    private String observations;

    @Column(name = "created_by_user_id", nullable = false)
    private UUID createdByUserId;

    @Column(name = "created_by_full_name", length = 160)
    private String createdByFullName;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Version
    private Long version;
}
