package cm.ftg.tontine.auditor.clarification.dto;

import cm.ftg.tontine.auditor.clarification.enums.ClarificationEvaluation;
import jakarta.validation.constraints.NotNull;

public record EvaluateClarificationRequest(
        @NotNull ClarificationEvaluation evaluation
) {
}
