package cm.ftg.tontine.auditor.audit.dto;

import cm.ftg.tontine.auditor.audit.entity.Audit;
import cm.ftg.tontine.auditor.audit.enums.AuditFinding;
import cm.ftg.tontine.auditor.audit.enums.AuditScope;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record AuditDto(
        UUID id,
        UUID tontineId,
        AuditScope scope,
        LocalDate periodFrom,
        LocalDate periodTo,
        AuditFinding overallFinding,
        String observations,
        UUID createdByUserId,
        String createdByFullName,
        Instant createdAt,
        List<AuditFindingDto> findings
) {

    public static AuditDto from(Audit a, List<AuditFindingDto> findings) {
        return new AuditDto(
                a.getId(), a.getTontineId(), a.getScope(),
                a.getPeriodFrom(), a.getPeriodTo(), a.getOverallFinding(),
                a.getObservations(), a.getCreatedByUserId(), a.getCreatedByFullName(),
                a.getCreatedAt(), findings);
    }
}
