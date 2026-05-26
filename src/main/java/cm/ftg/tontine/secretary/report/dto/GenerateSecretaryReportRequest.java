package cm.ftg.tontine.secretary.report.dto;

import cm.ftg.tontine.secretary.report.enums.SecretaryReportCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record GenerateSecretaryReportRequest(
        @NotNull SecretaryReportCategory category,
        @NotBlank @Size(max = 80) String periodLabel
) {
}
