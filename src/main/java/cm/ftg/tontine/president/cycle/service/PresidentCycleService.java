package cm.ftg.tontine.president.cycle.service;

import cm.ftg.tontine.audit.service.AuditService;
import cm.ftg.tontine.common.exception.ApiException;
import cm.ftg.tontine.common.exception.ResourceNotFoundException;
import cm.ftg.tontine.president.security.PresidentAccessChecker;
import cm.ftg.tontine.secretary.session.dto.SecretaryCycleDto;
import cm.ftg.tontine.tontine.entity.Cycle;
import cm.ftg.tontine.tontine.enums.CycleStatus;
import cm.ftg.tontine.tontine.repository.CycleRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PresidentCycleService {

    private final CycleRepository cycleRepository;
    private final PresidentAccessChecker accessChecker;
    private final AuditService auditService;

    public PresidentCycleService(CycleRepository cycleRepository,
                                 PresidentAccessChecker accessChecker,
                                 AuditService auditService) {
        this.cycleRepository = cycleRepository;
        this.accessChecker = accessChecker;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<SecretaryCycleDto> listPendingClosures(UUID tontineId, UUID userId) {
        accessChecker.requirePresident(userId, tontineId);
        return cycleRepository.findByTontineIdOrderByNumberAsc(tontineId).stream()
                .filter(c -> c.getStatus() == CycleStatus.CLOSURE_REQUESTED)
                .map(SecretaryCycleDto::from)
                .toList();
    }

    @Transactional
    public SecretaryCycleDto closeCycle(UUID cycleId, UUID tontineId, UUID userId) {
        accessChecker.requirePresident(userId, tontineId);
        Cycle cycle = cycleRepository.findById(cycleId)
                .orElseThrow(() -> new ResourceNotFoundException("Cycle", cycleId));
        if (!cycle.getTontineId().equals(tontineId)) {
            throw new ApiException("FORBIDDEN", "Cycle hors tontine", HttpStatus.FORBIDDEN);
        }
        if (cycle.getStatus() != CycleStatus.CLOSURE_REQUESTED) {
            throw new ApiException("CYCLE_INVALID_STATE",
                    "Seul un cycle en attente de clôture (CLOSURE_REQUESTED) peut être clôturé.",
                    HttpStatus.CONFLICT);
        }
        cycle.setStatus(CycleStatus.CLOSED);
        cycle.setEndDate(LocalDate.now());
        Cycle saved = cycleRepository.save(cycle);
        auditService.record(userId, "CYCLE_CLOSED", "Cycle", cycleId.toString(), tontineId, null);
        return SecretaryCycleDto.from(saved);
    }
}
