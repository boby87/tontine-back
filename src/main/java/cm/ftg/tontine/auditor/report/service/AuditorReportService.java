package cm.ftg.tontine.auditor.report.service;

import cm.ftg.tontine.audit.service.AuditService;
import cm.ftg.tontine.auditor.report.dto.AuditorExportResponseDto;
import cm.ftg.tontine.auditor.report.dto.GenerateAuditorReportRequest;
import cm.ftg.tontine.auditor.security.AuditorAccessChecker;
import cm.ftg.tontine.member.entity.Member;
import cm.ftg.tontine.president.report.dto.ReportEntryDto;
import cm.ftg.tontine.president.report.entity.ReportEntry;
import cm.ftg.tontine.president.report.enums.ReportCategory;
import cm.ftg.tontine.president.report.repository.ReportEntryRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditorReportService {

    private final ReportEntryRepository repository;
    private final AuditorAccessChecker accessChecker;
    private final AuditService auditService;

    public AuditorReportService(ReportEntryRepository repository,
                                AuditorAccessChecker accessChecker,
                                AuditService auditService) {
        this.repository = repository;
        this.accessChecker = accessChecker;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<ReportEntryDto> list(UUID tontineId, UUID userId) {
        accessChecker.requireAuditor(userId, tontineId);
        return repository
                .findByTontineIdAndCategoryOrderByGeneratedAtDesc(tontineId, ReportCategory.AUDIT)
                .stream()
                .map(ReportEntryDto::from)
                .toList();
    }

    @Transactional
    public ReportEntryDto generate(UUID tontineId, UUID userId, GenerateAuditorReportRequest req) {
        Member auditor = accessChecker.requireAuditor(userId, tontineId);
        ReportEntry entry = new ReportEntry();
        entry.setTontineId(tontineId);
        entry.setCategory(ReportCategory.AUDIT);
        entry.setTitle("Rapport commissaire " + req.scope() + " - " + req.periodLabel());
        entry.setPeriodLabel(req.periodLabel());
        entry.setAuthorFullName(auditor.getFirstName() + " " + auditor.getLastName());
        entry.setMetricsJson("");
        if (req.observations() != null && !req.observations().isBlank()) {
            entry.setDescription(req.observations());
        }
        ReportEntry saved = repository.save(entry);
        auditService.record(userId, "AUDITOR_REPORT_GENERATE", "ReportEntry",
                saved.getId().toString(), tontineId,
                "{\"scope\":\"" + req.scope().name() + "\"}");
        return ReportEntryDto.from(saved);
    }

    @Transactional
    public AuditorExportResponseDto export(UUID tontineId, UUID userId, String dataset) {
        accessChecker.requireAuditor(userId, tontineId);
        String safeDataset = (dataset == null || dataset.isBlank()) ? "default" : dataset;
        UUID fileId = UUID.randomUUID();
        AuditorExportResponseDto dto = new AuditorExportResponseDto(
                safeDataset,
                Instant.now(),
                "/files/exports/" + fileId + ".xlsx",
                "/files/exports/" + fileId + ".csv",
                "/files/exports/" + fileId + ".pdf",
                0L);
        auditService.record(userId, "AUDITOR_EXPORT", "AuditorExport",
                fileId.toString(), tontineId,
                "{\"dataset\":\"" + safeDataset + "\"}");
        return dto;
    }
}
