package cm.ftg.tontine.auditor.clarification.dto;

import cm.ftg.tontine.auditor.clarification.entity.Clarification;
import cm.ftg.tontine.auditor.clarification.enums.ClarificationEvaluation;
import cm.ftg.tontine.auditor.clarification.enums.ClarificationStatus;
import cm.ftg.tontine.common.enums.UserRole;
import java.time.Instant;
import java.util.UUID;

public record ClarificationDto(
        UUID id,
        UUID tontineId,
        String subject,
        String question,
        UserRole targetRole,
        int dueWithinHours,
        UUID askedByUserId,
        String askedByFullName,
        Instant askedAt,
        ClarificationStatus status,
        String response,
        Instant respondedAt,
        ClarificationEvaluation evaluation,
        Instant evaluatedAt
) {

    public static ClarificationDto from(Clarification c) {
        return new ClarificationDto(
                c.getId(), c.getTontineId(), c.getSubject(), c.getQuestion(),
                c.getTargetRole(), c.getDueWithinHours(), c.getAskedByUserId(),
                c.getAskedByFullName(), c.getAskedAt(), c.getStatus(), c.getResponse(),
                c.getRespondedAt(), c.getEvaluation(), c.getEvaluatedAt());
    }
}
