package cm.ftg.tontine.president.conflict.dto;

import cm.ftg.tontine.president.conflict.entity.Conflict;
import cm.ftg.tontine.president.conflict.enums.ConflictDecisionOutcome;
import cm.ftg.tontine.president.conflict.enums.ConflictSeverity;
import cm.ftg.tontine.president.conflict.enums.ConflictStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ConflictDto(
        UUID id,
        UUID tontineId,
        String subject,
        String description,
        List<ConflictPartyDto> parties,
        ConflictStatus status,
        UUID escalatedByUserId,
        String escalatedByFullName,
        Instant escalatedAt,
        ConflictSeverity severity,
        ConflictDecisionOutcome decisionOutcome,
        String decisionComment,
        Instant decidedAt,
        Instant mediationScheduledAt,
        String mediationNote
) {

    public static ConflictDto from(Conflict c, List<ConflictPartyDto> parties) {
        return new ConflictDto(
                c.getId(), c.getTontineId(), c.getSubject(), c.getDescription(),
                parties, c.getStatus(),
                c.getEscalatedByUserId(), c.getEscalatedByFullName(), c.getEscalatedAt(),
                c.getSeverity(), c.getDecisionOutcome(), c.getDecisionComment(),
                c.getDecidedAt(), c.getMediationScheduledAt(), c.getMediationNote());
    }
}
