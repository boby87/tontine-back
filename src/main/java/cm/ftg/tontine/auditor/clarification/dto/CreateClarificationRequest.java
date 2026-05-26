package cm.ftg.tontine.auditor.clarification.dto;

import cm.ftg.tontine.common.enums.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateClarificationRequest(
        @NotBlank @Size(max = 160) String subject,
        @NotBlank @Size(max = 4000) String question,
        @NotNull UserRole targetRole,
        @Positive int dueWithinHours
) {
}
