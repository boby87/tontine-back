package cm.ftg.tontine.auditor.audit.dto;

import cm.ftg.tontine.auditor.audit.entity.AuditFindingEntry;
import cm.ftg.tontine.auditor.audit.enums.AuditFinding;
import java.util.UUID;

public record AuditFindingDto(
        UUID id,
        UUID auditId,
        String area,
        AuditFinding finding,
        String description,
        int orderIdx
) {

    public static AuditFindingDto from(AuditFindingEntry e) {
        return new AuditFindingDto(
                e.getId(), e.getAuditId(), e.getArea(),
                e.getFinding(), e.getDescription(), e.getOrderIdx());
    }
}
