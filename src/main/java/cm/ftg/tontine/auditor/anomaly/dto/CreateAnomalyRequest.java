package cm.ftg.tontine.auditor.anomaly.dto;

import cm.ftg.tontine.auditor.anomaly.enums.AnomalyAudience;
import cm.ftg.tontine.auditor.anomaly.enums.AnomalyCategory;
import cm.ftg.tontine.auditor.anomaly.enums.AnomalySeverity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateAnomalyRequest(
        @NotNull AnomalyCategory category,
        @NotNull AnomalySeverity severity,
        @NotBlank @Size(max = 160) String title,
        @NotBlank @Size(max = 4000) String description,
        @NotNull AnomalyAudience audience,
        Boolean requestsResponse,
        Boolean copyToTreasurer
) {
}
