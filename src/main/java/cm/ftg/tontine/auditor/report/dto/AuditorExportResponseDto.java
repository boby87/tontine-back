package cm.ftg.tontine.auditor.report.dto;

import java.time.Instant;

public record AuditorExportResponseDto(
        String dataset,
        Instant generatedAt,
        String downloadUrlExcel,
        String downloadUrlCsv,
        String downloadUrlPdf,
        long recordCount
) {
}
