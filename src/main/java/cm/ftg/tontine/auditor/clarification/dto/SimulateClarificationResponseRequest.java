package cm.ftg.tontine.auditor.clarification.dto;

import jakarta.validation.constraints.Size;

public record SimulateClarificationResponseRequest(
        @Size(max = 4000) String response
) {
}
