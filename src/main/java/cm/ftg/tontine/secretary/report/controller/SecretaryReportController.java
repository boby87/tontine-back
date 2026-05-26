package cm.ftg.tontine.secretary.report.controller;

import cm.ftg.tontine.audit.service.AuditService;
import cm.ftg.tontine.common.dto.ApiResponse;
import cm.ftg.tontine.member.entity.Member;
import cm.ftg.tontine.president.common.TontineIdResolver;
import cm.ftg.tontine.president.report.dto.ReportEntryDto;
import cm.ftg.tontine.president.report.entity.ReportEntry;
import cm.ftg.tontine.president.report.enums.ReportCategory;
import cm.ftg.tontine.president.report.repository.ReportEntryRepository;
import cm.ftg.tontine.secretary.report.dto.GenerateSecretaryReportRequest;
import cm.ftg.tontine.secretary.report.enums.SecretaryReportCategory;
import cm.ftg.tontine.secretary.security.SecretaryAccessChecker;
import cm.ftg.tontine.security.AuthenticatedUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/secretary/reports")
public class SecretaryReportController {

    private final ReportEntryRepository repository;
    private final SecretaryAccessChecker accessChecker;
    private final TontineIdResolver tontineIdResolver;
    private final AuditService auditService;

    public SecretaryReportController(ReportEntryRepository repository,
                                     SecretaryAccessChecker accessChecker,
                                     TontineIdResolver tontineIdResolver,
                                     AuditService auditService) {
        this.repository = repository;
        this.accessChecker = accessChecker;
        this.tontineIdResolver = tontineIdResolver;
        this.auditService = auditService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ApiResponse<List<ReportEntryDto>> list(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        accessChecker.requireSecretary(user.id(), t);
        return ApiResponse.ok(repository.findByTontineIdOrderByGeneratedAtDesc(t).stream()
                .map(ReportEntryDto::from)
                .toList());
    }

    @PostMapping("/generate")
    @Transactional
    public ApiResponse<ReportEntryDto> generate(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId,
            @Valid @RequestBody GenerateSecretaryReportRequest req) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        Member secretary = accessChecker.requireSecretary(user.id(), t);
        ReportEntry r = new ReportEntry();
        r.setTontineId(t);
        // ATTENDANCE and MEMBERSHIP map to CENSOR (closest existing category in ReportCategory enum)
        r.setCategory(mapCategory(req.category()));
        r.setTitle("Rapport " + req.category().name() + " - " + req.periodLabel());
        r.setPeriodLabel(req.periodLabel());
        r.setAuthorFullName(buildFullName(secretary));
        ReportEntry saved = repository.save(r);
        auditService.record(user.id(), "REPORT_GENERATE", "ReportEntry", saved.getId().toString(),
                t, "{\"category\":\"" + req.category().name() + "\"}");
        return ApiResponse.ok(ReportEntryDto.from(saved), "Rapport genere");
    }

    private ReportCategory mapCategory(SecretaryReportCategory c) {
        return switch (c) {
            case PERIODIC -> ReportCategory.PERIODIC;
            case CYCLE -> ReportCategory.CYCLE;
            case ATTENDANCE, MEMBERSHIP -> ReportCategory.CENSOR;
        };
    }

    private String buildFullName(Member m) {
        String first = m.getFirstName() == null ? "" : m.getFirstName();
        String last = m.getLastName() == null ? "" : m.getLastName();
        String full = (first + " " + last).trim();
        return full.isEmpty() ? m.getMatricule() : full;
    }
}
