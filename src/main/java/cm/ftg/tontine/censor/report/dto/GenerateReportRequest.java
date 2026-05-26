package cm.ftg.tontine.censor.report.dto;

import cm.ftg.tontine.censor.report.enums.CensorReportScope;
import jakarta.validation.constraints.NotNull;

public record GenerateReportRequest(
        @NotNull CensorReportScope scope,
        String periodLabel,
        String observations
) {}
