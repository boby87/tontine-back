package cm.ftg.tontine.treasurer.report.service;

import cm.ftg.tontine.audit.service.AuditService;
import cm.ftg.tontine.member.entity.Member;
import cm.ftg.tontine.president.report.dto.ReportEntryDto;
import cm.ftg.tontine.president.report.entity.ReportEntry;
import cm.ftg.tontine.president.report.enums.ReportCategory;
import cm.ftg.tontine.president.report.repository.ReportEntryRepository;
import cm.ftg.tontine.treasurer.report.dto.GenerateTreasurerReportRequest;
import cm.ftg.tontine.treasurer.security.TreasurerAccessChecker;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TreasurerReportService {

    private final ReportEntryRepository repository;
    private final TreasurerAccessChecker accessChecker;
    private final AuditService auditService;

    public TreasurerReportService(ReportEntryRepository repository,
                                   TreasurerAccessChecker accessChecker,
                                   AuditService auditService) {
        this.repository = repository;
        this.accessChecker = accessChecker;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<ReportEntryDto> list(UUID tontineId, UUID userId) {
        accessChecker.requireTreasurer(userId, tontineId);
        return repository
                .findByTontineIdAndCategoryOrderByGeneratedAtDesc(tontineId, ReportCategory.TREASURY)
                .stream()
                .map(ReportEntryDto::from)
                .toList();
    }

    @Transactional
    public ReportEntryDto generate(UUID tontineId, UUID userId,
                                    GenerateTreasurerReportRequest req) {
        Member treasurer = accessChecker.requireTreasurer(userId, tontineId);

        ReportEntry entry = new ReportEntry();
        entry.setTontineId(tontineId);
        entry.setCategory(ReportCategory.TREASURY);
        entry.setTitle("Rapport tresorier " + req.type() + " - " + req.periodLabel());
        entry.setPeriodLabel(req.periodLabel());
        entry.setAuthorFullName(treasurer.getFirstName() + " " + treasurer.getLastName());
        entry.setMetricsJson("");
        ReportEntry saved = repository.save(entry);

        auditService.record(userId, "TREASURER_REPORT_GENERATE", "ReportEntry",
                saved.getId().toString(), tontineId,
                "{\"type\":\"" + req.type() + "\"}");
        return ReportEntryDto.from(saved);
    }
}
