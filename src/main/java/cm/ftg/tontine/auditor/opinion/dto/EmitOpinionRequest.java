package cm.ftg.tontine.auditor.opinion.dto;

import cm.ftg.tontine.president.validation.enums.AuditorOpinionStatus;
import jakarta.validation.constraints.NotNull;

public record EmitOpinionRequest(
        @NotNull AuditorOpinionStatus status,
        String comment
) {
}
