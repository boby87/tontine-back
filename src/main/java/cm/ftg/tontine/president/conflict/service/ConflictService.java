package cm.ftg.tontine.president.conflict.service;

import cm.ftg.tontine.audit.service.AuditService;
import cm.ftg.tontine.common.exception.ApiException;
import cm.ftg.tontine.common.exception.ResourceNotFoundException;
import cm.ftg.tontine.president.conflict.dto.ConflictDecisionRequest;
import cm.ftg.tontine.president.conflict.dto.ConflictDto;
import cm.ftg.tontine.president.conflict.dto.ConflictPartyDto;
import cm.ftg.tontine.president.conflict.dto.ScheduleMediationRequest;
import cm.ftg.tontine.president.conflict.entity.Conflict;
import cm.ftg.tontine.president.conflict.enums.ConflictStatus;
import cm.ftg.tontine.president.conflict.repository.ConflictPartyRepository;
import cm.ftg.tontine.president.conflict.repository.ConflictRepository;
import cm.ftg.tontine.president.security.PresidentAccessChecker;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConflictService {

    private final ConflictRepository repository;
    private final ConflictPartyRepository partyRepository;
    private final PresidentAccessChecker accessChecker;
    private final AuditService auditService;

    public ConflictService(ConflictRepository repository,
                           ConflictPartyRepository partyRepository,
                           PresidentAccessChecker accessChecker,
                           AuditService auditService) {
        this.repository = repository;
        this.partyRepository = partyRepository;
        this.accessChecker = accessChecker;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<ConflictDto> list(UUID tontineId, UUID userId) {
        accessChecker.requirePresident(userId, tontineId);
        return repository.findByTontineIdOrderByEscalatedAtDesc(tontineId).stream()
                .map(c -> ConflictDto.from(c, loadParties(c.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public ConflictDto findById(UUID id, UUID tontineId, UUID userId) {
        accessChecker.requirePresident(userId, tontineId);
        Conflict c = loadInTontine(id, tontineId);
        return ConflictDto.from(c, loadParties(c.getId()));
    }

    @Transactional
    public ConflictDto scheduleMediation(UUID id, UUID tontineId, UUID userId, ScheduleMediationRequest req) {
        accessChecker.requirePresident(userId, tontineId);
        Conflict c = loadInTontine(id, tontineId);
        if (c.getStatus() != ConflictStatus.OPEN && c.getStatus() != ConflictStatus.MEDIATION_SCHEDULED) {
            throw new ApiException("CONFLICT_INVALID_STATE",
                    "La mediation ne peut etre planifiee que pour un conflit ouvert", HttpStatus.CONFLICT);
        }
        c.setStatus(ConflictStatus.MEDIATION_SCHEDULED);
        c.setMediationScheduledAt(req.scheduledAt());
        c.setMediationNote(req.note());
        Conflict saved = repository.save(c);
        auditService.record(userId, "CONFLICT_SCHEDULE_MEDIATION", "Conflict", id.toString(),
                tontineId, null);
        return ConflictDto.from(saved, loadParties(saved.getId()));
    }

    @Transactional
    public ConflictDto decide(UUID id, UUID tontineId, UUID userId, ConflictDecisionRequest req) {
        accessChecker.requirePresident(userId, tontineId);
        Conflict c = loadInTontine(id, tontineId);
        if (c.getStatus() == ConflictStatus.CLOSED || c.getStatus() == ConflictStatus.DECIDED_BY_PRESIDENT) {
            throw new ApiException("CONFLICT_FINAL_STATE",
                    "Ce conflit a deja une decision presidentielle", HttpStatus.CONFLICT);
        }
        c.setStatus(ConflictStatus.DECIDED_BY_PRESIDENT);
        c.setDecisionOutcome(req.outcome());
        c.setDecisionComment(req.comment());
        c.setDecidedAt(Instant.now());
        Conflict saved = repository.save(c);
        auditService.record(userId, "CONFLICT_DECIDE", "Conflict", id.toString(),
                tontineId, "{\"outcome\":\"" + req.outcome().name() + "\"}");
        return ConflictDto.from(saved, loadParties(saved.getId()));
    }

    private Conflict loadInTontine(UUID id, UUID tontineId) {
        Conflict c = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conflict", id));
        if (!c.getTontineId().equals(tontineId)) {
            throw new ApiException("FORBIDDEN", "Tontine non concordante", HttpStatus.FORBIDDEN);
        }
        return c;
    }

    private List<ConflictPartyDto> loadParties(UUID conflictId) {
        return partyRepository.findByConflictIdOrderByFullNameAsc(conflictId).stream()
                .map(ConflictPartyDto::from)
                .toList();
    }
}
