package cm.ftg.tontine.auditor.recommendation.dto;

import cm.ftg.tontine.auditor.recommendation.entity.Recommendation;
import cm.ftg.tontine.auditor.recommendation.enums.RecommendationOrigin;
import cm.ftg.tontine.auditor.recommendation.enums.RecommendationPriority;
import cm.ftg.tontine.auditor.recommendation.enums.RecommendationRecipient;
import cm.ftg.tontine.auditor.recommendation.enums.RecommendationStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record RecommendationDto(
        UUID id,
        UUID tontineId,
        RecommendationOrigin origin,
        UUID originId,
        String title,
        String description,
        RecommendationPriority priority,
        RecommendationRecipient recipient,
        LocalDate dueDate,
        RecommendationStatus status,
        int progress,
        String closeNote,
        UUID createdByUserId,
        String createdByFullName,
        Instant createdAt,
        Instant updatedAt
) {

    public static RecommendationDto from(Recommendation r) {
        return new RecommendationDto(
                r.getId(), r.getTontineId(), r.getOrigin(), r.getOriginId(),
                r.getTitle(), r.getDescription(), r.getPriority(), r.getRecipient(),
                r.getDueDate(), r.getStatus(), r.getProgress(), r.getCloseNote(),
                r.getCreatedByUserId(), r.getCreatedByFullName(),
                r.getCreatedAt(), r.getUpdatedAt());
    }
}
