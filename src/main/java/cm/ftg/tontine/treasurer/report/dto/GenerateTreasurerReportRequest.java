package cm.ftg.tontine.treasurer.report.dto;

import cm.ftg.tontine.treasurer.report.enums.TreasurerReportType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record GenerateTreasurerReportRequest(
        @NotBlank String periodLabel,
        @NotNull TreasurerReportType type
) {
}
