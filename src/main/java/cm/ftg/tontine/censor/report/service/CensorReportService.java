package cm.ftg.tontine.censor.report.service;

import cm.ftg.tontine.audit.service.AuditService;
import cm.ftg.tontine.censor.report.dto.GenerateReportRequest;
import cm.ftg.tontine.censor.report.dto.UpdateObservationsRequest;
import cm.ftg.tontine.censor.security.CensorAccessChecker;
import cm.ftg.tontine.common.exception.ApiException;
import cm.ftg.tontine.common.exception.ResourceNotFoundException;
import cm.ftg.tontine.member.entity.Member;
import cm.ftg.tontine.president.report.dto.ReportEntryDto;
import cm.ftg.tontine.president.report.entity.ReportEntry;
import cm.ftg.tontine.president.report.enums.ReportCategory;
import cm.ftg.tontine.president.report.repository.ReportEntryRepository;
import cm.ftg.tontine.president.sanction.enums.SanctionStatus;
import cm.ftg.tontine.president.sanction.repository.SanctionRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CensorReportService {

    private final ReportEntryRepository reportRepository;
    private final SanctionRepository sanctionRepository;
    private final CensorAccessChecker accessChecker;
    private final AuditService auditService;

    public CensorReportService(ReportEntryRepository reportRepository,
                               SanctionRepository sanctionRepository,
                               CensorAccessChecker accessChecker,
                               AuditService auditService) {
        this.reportRepository = reportRepository;
        this.sanctionRepository = sanctionRepository;
        this.accessChecker = accessChecker;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<ReportEntryDto> list(UUID tontineId, UUID userId) {
        accessChecker.requireCensor(userId, tontineId);
        return reportRepository
                .findByTontineIdAndCategoryOrderByGeneratedAtDesc(tontineId, ReportCategory.CENSOR)
                .stream()
                .map(ReportEntryDto::from)
                .toList();
    }

    @Transactional
    public ReportEntryDto generate(UUID tontineId, UUID userId, GenerateReportRequest req) {
        Member censor = accessChecker.requireCensor(userId, tontineId);
        String periodLabel = (req.periodLabel() == null || req.periodLabel().isBlank())
                ? defaultPeriodLabel(req)
                : req.periodLabel();

        long totalSanctions = sanctionRepository
                .findByTontineIdOrderByIssuedAtDesc(tontineId).size();
        BigDecimal totalAmount = sanctionRepository.sumAmountByTontineIdAndStatus(tontineId, SanctionStatus.CONFIRMED);
        long unpaidCount = sanctionRepository.countByTontineIdAndStatus(tontineId, SanctionStatus.CONFIRMED);
        BigDecimal unpaidAmount = sanctionRepository.sumAmountByTontineIdAndStatus(tontineId, SanctionStatus.CONFIRMED);

        ReportEntry entry = new ReportEntry();
        entry.setTontineId(tontineId);
        entry.setCategory(ReportCategory.CENSOR);
        entry.setTitle("Rapport censeur " + req.scope() + " - " + periodLabel);
        entry.setDescription(req.observations());
        entry.setPeriodLabel(periodLabel);
        entry.setAuthorFullName(fullName(censor));
        entry.setMetricsJson("{\"totalSanctions\":" + totalSanctions
                + ",\"totalAmount\":" + (totalAmount == null ? BigDecimal.ZERO : totalAmount)
                + ",\"unpaidCount\":" + unpaidCount
                + ",\"unpaidAmount\":" + (unpaidAmount == null ? BigDecimal.ZERO : unpaidAmount)
                + ",\"scope\":\"" + req.scope() + "\"}");
        ReportEntry saved = reportRepository.save(entry);
        auditService.record(userId, "CENSOR_REPORT_GENERATE", "ReportEntry", saved.getId().toString(), tontineId,
                "{\"scope\":\"" + req.scope() + "\",\"periodLabel\":\"" + escape(periodLabel) + "\"}");
        return ReportEntryDto.from(saved);
    }

    @Transactional
    public ReportEntryDto updateObservations(UUID id, UUID tontineId, UUID userId, UpdateObservationsRequest req) {
        accessChecker.requireCensor(userId, tontineId);
        ReportEntry r = reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report", id));
        if (!r.getTontineId().equals(tontineId)) {
            throw new ApiException("FORBIDDEN", "Tontine non concordante", HttpStatus.FORBIDDEN);
        }
        if (r.getCategory() != ReportCategory.CENSOR) {
            throw new ApiException("REPORT_INVALID_CATEGORY",
                    "Rapport non gere par le censeur", HttpStatus.CONFLICT);
        }
        r.setDescription(req.observations());
        ReportEntry saved = reportRepository.save(r);
        auditService.record(userId, "CENSOR_REPORT_OBSERVATIONS", "ReportEntry", id.toString(), tontineId, null);
        return ReportEntryDto.from(saved);
    }

    private String defaultPeriodLabel(GenerateReportRequest req) {
        return switch (req.scope()) {
            case LAST_SESSION -> "Derniere session";
            case CUSTOM_RANGE -> "Periode personnalisee";
            case CYCLE -> "Cycle";
        };
    }

    private String fullName(Member m) {
        return (m.getFirstName() == null ? "" : m.getFirstName()) + " "
                + (m.getLastName() == null ? "" : m.getLastName());
    }

    private String escape(String s) {
        return s == null ? "" : s.replace("\"", "\\\"");
    }
}
