package cm.ftg.tontine.auditor.recommendation.dto;

import cm.ftg.tontine.auditor.recommendation.enums.RecommendationOrigin;
import cm.ftg.tontine.auditor.recommendation.enums.RecommendationPriority;
import cm.ftg.tontine.auditor.recommendation.enums.RecommendationRecipient;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

public record CreateRecommendationRequest(
        @NotNull RecommendationOrigin origin,
        UUID originId,
        @NotBlank @Size(max = 160) String title,
        @NotBlank @Size(max = 4000) String description,
        @NotNull RecommendationPriority priority,
        @NotNull RecommendationRecipient recipient,
        LocalDate dueDate
) {
}
