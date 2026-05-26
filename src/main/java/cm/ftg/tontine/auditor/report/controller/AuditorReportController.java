package cm.ftg.tontine.auditor.report.controller;

import cm.ftg.tontine.auditor.report.dto.AuditorExportResponseDto;
import cm.ftg.tontine.auditor.report.dto.GenerateAuditorReportRequest;
import cm.ftg.tontine.auditor.report.service.AuditorReportService;
import cm.ftg.tontine.common.dto.ApiResponse;
import cm.ftg.tontine.president.common.TontineIdResolver;
import cm.ftg.tontine.president.report.dto.ReportEntryDto;
import cm.ftg.tontine.security.AuthenticatedUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuditorReportController {

    private final AuditorReportService service;
    private final TontineIdResolver tontineIdResolver;

    public AuditorReportController(AuditorReportService service,
                                   TontineIdResolver tontineIdResolver) {
        this.service = service;
        this.tontineIdResolver = tontineIdResolver;
    }

    @GetMapping("/auditor/reports")
    public ApiResponse<List<ReportEntryDto>> list(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.list(t, user.id()));
    }

    @PostMapping("/auditor/reports/generate")
    public ApiResponse<ReportEntryDto> generate(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId,
            @Valid @RequestBody GenerateAuditorReportRequest req) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.generate(t, user.id(), req), "Rapport genere");
    }

    @GetMapping("/auditor/export")
    public ApiResponse<AuditorExportResponseDto> export(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId,
            @RequestParam(value = "dataset", required = false) String dataset) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.export(t, user.id(), dataset), "Export prepare");
    }
}
