package cm.ftg.tontine.auditor.recommendation.dto;

import cm.ftg.tontine.auditor.recommendation.enums.RecommendationStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateRecommendationStatusRequest(
        @NotNull RecommendationStatus status,
        Integer progress,
        @Size(max = 2000) String closeNote
) {
}
