package cm.ftg.tontine.auditor.audit.entity;

import cm.ftg.tontine.auditor.audit.enums.AuditFinding;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "audit_findings", indexes = {
        @Index(name = "idx_audit_finding_audit", columnList = "audit_id")
})
@Getter
@Setter
@NoArgsConstructor
public class AuditFindingEntry {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "audit_id", nullable = false)
    private UUID auditId;

    @Column(nullable = false, length = 160)
    private String area;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuditFinding finding;

    @Column(length = 2000)
    private String description;

    @Column(name = "order_idx", nullable = false)
    private int orderIdx;
}
