package cm.ftg.tontine.audit.entity;

import cm.ftg.tontine.common.enums.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

/** Journal d'audit — jamais supprimé, pas de soft delete, pas de @SQLRestriction. */
@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_actor", columnList = "performed_by"),
        @Index(name = "idx_audit_target", columnList = "entity_type, entity_id"),
        @Index(name = "idx_audit_tontine", columnList = "tontine_id"),
        @Index(name = "idx_audit_performed_at", columnList = "performed_at")
})
@Getter
@Setter
@NoArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue
    private UUID id;

    @CreationTimestamp
    @Column(name = "performed_at", updatable = false, nullable = false)
    private Instant performedAt;

    /** UUID de l'utilisateur ayant effectué l'action. */
    @Column(name = "performed_by")
    private UUID performedBy;

    /** Rôle de l'utilisateur au moment de l'action (dans la tontine concernée). */
    @Enumerated(EnumType.STRING)
    @Column(name = "performed_by_role", length = 30)
    private UserRole performedByRole;

    /** Type d'action : CREATE, UPDATE, APPROVE, REJECT, SUSPEND, DELETE… */
    @Column(nullable = false, length = 80)
    private String action;

    /** Nom de l'entité concernée (ex. "Loan", "Member", "Payment"). */
    @Column(name = "entity_type", length = 80)
    private String entityType;

    /** ID de l'entité concernée. */
    @Column(name = "entity_id", length = 80)
    private String entityId;

    /** État JSON de l'entité avant l'action (JSONB). */
    @Column(name = "old_values", columnDefinition = "TEXT")
    private String oldValues;

    /** État JSON de l'entité après l'action (JSONB). */
    @Column(name = "new_values", columnDefinition = "TEXT")
    private String newValues;

    @Column(name = "tontine_id")
    private UUID tontineId;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 512)
    private String userAgent;
}
