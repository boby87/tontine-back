package cm.ftg.tontine.auditor.balancereview.dto;

import cm.ftg.tontine.auditor.balancereview.enums.SessionBalanceReviewDecision;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateBalanceReviewRequest(
        @NotNull UUID sessionId,
        @NotNull SessionBalanceReviewDecision decision,
        String observations,
        String reserves
) {
}
