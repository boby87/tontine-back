package cm.ftg.tontine.auditor.anomaly.dto;

import jakarta.validation.constraints.Size;

public record CloseAnomalyRequest(
        @Size(max = 2000) String resolutionComment
) {
}
