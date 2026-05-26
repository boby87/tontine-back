package cm.ftg.tontine.auditor.anomaly.dto;

import cm.ftg.tontine.auditor.anomaly.entity.Anomaly;
import cm.ftg.tontine.auditor.anomaly.enums.AnomalyAudience;
import cm.ftg.tontine.auditor.anomaly.enums.AnomalyCategory;
import cm.ftg.tontine.auditor.anomaly.enums.AnomalySeverity;
import cm.ftg.tontine.auditor.anomaly.enums.AnomalyStatus;
import java.time.Instant;
import java.util.UUID;

public record AnomalyDto(
        UUID id,
        UUID tontineId,
        AnomalyCategory category,
        AnomalySeverity severity,
        String title,
        String description,
        AnomalyAudience audience,
        AnomalyStatus status,
        boolean requestsResponse,
        boolean copyToTreasurer,
        UUID reportedByUserId,
        String reportedByFullName,
        Instant reportedAt,
        Instant resolvedAt,
        Instant closedAt,
        String resolutionComment
) {

    public static AnomalyDto from(Anomaly a) {
        return new AnomalyDto(
                a.getId(), a.getTontineId(), a.getCategory(), a.getSeverity(),
                a.getTitle(), a.getDescription(), a.getAudience(), a.getStatus(),
                a.isRequestsResponse(), a.isCopyToTreasurer(),
                a.getReportedByUserId(), a.getReportedByFullName(), a.getReportedAt(),
                a.getResolvedAt(), a.getClosedAt(), a.getResolutionComment());
    }
}
