package cm.ftg.tontine.president.report.dto;

import cm.ftg.tontine.president.report.entity.ReportEntry;
import cm.ftg.tontine.president.report.enums.ReportCategory;
import java.time.Instant;
import java.util.UUID;

public record ReportEntryDto(
        UUID id,
        UUID tontineId,
        ReportCategory category,
        String title,
        String description,
        String periodLabel,
        String authorFullName,
        Instant generatedAt,
        String metricsJson,
        String downloadUrlPdf,
        String downloadUrlExcel
) {

    public static ReportEntryDto from(ReportEntry r) {
        return new ReportEntryDto(
                r.getId(), r.getTontineId(), r.getCategory(), r.getTitle(), r.getDescription(),
                r.getPeriodLabel(), r.getAuthorFullName(), r.getGeneratedAt(), r.getMetricsJson(),
                r.getDownloadUrlPdf(), r.getDownloadUrlExcel());
    }
}
