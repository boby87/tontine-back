package cm.ftg.tontine.auditor.certification.dto;

import cm.ftg.tontine.auditor.certification.entity.Certification;
import cm.ftg.tontine.auditor.certification.enums.CertificationDecision;
import cm.ftg.tontine.auditor.certification.enums.CertificationScope;
import java.time.Instant;
import java.util.UUID;

public record CertificationDto(
        UUID id,
        UUID tontineId,
        CertificationScope scope,
        String periodLabel,
        CertificationDecision decision,
        String reserves,
        UUID issuedByUserId,
        String issuedByFullName,
        Instant issuedAt
) {

    public static CertificationDto from(Certification c) {
        return new CertificationDto(
                c.getId(), c.getTontineId(), c.getScope(), c.getPeriodLabel(),
                c.getDecision(), c.getReserves(), c.getIssuedByUserId(),
                c.getIssuedByFullName(), c.getIssuedAt());
    }
}
