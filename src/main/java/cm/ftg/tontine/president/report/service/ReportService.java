package cm.ftg.tontine.president.report.service;

import cm.ftg.tontine.common.exception.ApiException;
import cm.ftg.tontine.common.exception.ResourceNotFoundException;
import cm.ftg.tontine.president.report.dto.ReportEntryDto;
import cm.ftg.tontine.president.report.entity.ReportEntry;
import cm.ftg.tontine.president.report.enums.ReportCategory;
import cm.ftg.tontine.president.report.repository.ReportEntryRepository;
import cm.ftg.tontine.president.security.PresidentAccessChecker;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportService {

    private final ReportEntryRepository repository;
    private final PresidentAccessChecker accessChecker;

    public ReportService(ReportEntryRepository repository,
                         PresidentAccessChecker accessChecker) {
        this.repository = repository;
        this.accessChecker = accessChecker;
    }

    @Transactional(readOnly = true)
    public List<ReportEntryDto> list(UUID tontineId, UUID userId, ReportCategory category) {
        accessChecker.requirePresident(userId, tontineId);
        List<ReportEntry> entries = (category == null)
                ? repository.findByTontineIdOrderByGeneratedAtDesc(tontineId)
                : repository.findByTontineIdAndCategoryOrderByGeneratedAtDesc(tontineId, category);
        return entries.stream().map(ReportEntryDto::from).toList();
    }

    @Transactional(readOnly = true)
    public ReportEntryDto findById(UUID id, UUID tontineId, UUID userId) {
        accessChecker.requirePresident(userId, tontineId);
        ReportEntry r = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report", id));
        if (!r.getTontineId().equals(tontineId)) {
            throw new ApiException("FORBIDDEN", "Tontine non concordante", HttpStatus.FORBIDDEN);
        }
        return ReportEntryDto.from(r);
    }
}
