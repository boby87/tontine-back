package cm.ftg.tontine.auditor.report.dto;

import cm.ftg.tontine.auditor.report.enums.AuditorReportScope;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record GenerateAuditorReportRequest(
        @NotNull AuditorReportScope scope,
        @NotBlank @Size(max = 80) String periodLabel,
        @Size(max = 2000) String observations
) {
}
