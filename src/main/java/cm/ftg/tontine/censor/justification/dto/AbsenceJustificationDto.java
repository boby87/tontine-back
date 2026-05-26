package cm.ftg.tontine.censor.justification.dto;

import cm.ftg.tontine.censor.justification.entity.AbsenceJustification;
import cm.ftg.tontine.censor.justification.enums.JustificationStatus;
import java.time.Instant;
import java.util.UUID;

public record AbsenceJustificationDto(
        UUID id,
        UUID tontineId,
        UUID sessionId,
        Integer sessionNumber,
        UUID memberId,
        String memberFullName,
        String documentName,
        String documentType,
        Integer documentSizeKb,
        String reason,
        Instant submittedAt,
        JustificationStatus status,
        String censorComment,
        Instant censorDecidedAt,
        String presidentComment,
        Instant presidentDecidedAt,
        UUID linkedSanctionId
) {

    public static AbsenceJustificationDto from(AbsenceJustification j) {
        return new AbsenceJustificationDto(
                j.getId(), j.getTontineId(), j.getSessionId(), j.getSessionNumber(),
                j.getMemberId(), j.getMemberFullName(), j.getDocumentName(), j.getDocumentType(),
                j.getDocumentSizeKb(), j.getReason(), j.getSubmittedAt(), j.getStatus(),
                j.getCensorComment(), j.getCensorDecidedAt(), j.getPresidentComment(),
                j.getPresidentDecidedAt(), j.getLinkedSanctionId());
    }
}
