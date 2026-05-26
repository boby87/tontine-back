package cm.ftg.tontine.auditor.balancereview.dto;

import cm.ftg.tontine.auditor.balancereview.entity.SessionBalanceReview;
import cm.ftg.tontine.auditor.balancereview.enums.SessionBalanceReviewDecision;
import java.time.Instant;
import java.util.UUID;

public record SessionBalanceReviewDto(
        UUID id,
        UUID tontineId,
        UUID sessionId,
        Integer sessionNumber,
        SessionBalanceReviewDecision decision,
        String observations,
        String reserves,
        UUID reviewedByUserId,
        String reviewedByFullName,
        Instant reviewedAt
) {

    public static SessionBalanceReviewDto from(SessionBalanceReview r) {
        return new SessionBalanceReviewDto(
                r.getId(), r.getTontineId(), r.getSessionId(), r.getSessionNumber(),
                r.getDecision(), r.getObservations(), r.getReserves(),
                r.getReviewedByUserId(), r.getReviewedByFullName(), r.getReviewedAt());
    }
}
