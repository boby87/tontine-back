package cm.ftg.tontine.president.cycleclose.service;

import cm.ftg.tontine.audit.service.AuditService;
import cm.ftg.tontine.common.exception.ApiException;
import cm.ftg.tontine.common.exception.ResourceNotFoundException;
import cm.ftg.tontine.president.cycleclose.dto.CycleCloseChecklistItemDto;
import cm.ftg.tontine.president.cycleclose.dto.CycleCloseDto;
import cm.ftg.tontine.president.cycleclose.dto.SignCycleCloseRequest;
import cm.ftg.tontine.president.cycleclose.entity.CycleClose;
import cm.ftg.tontine.president.cycleclose.entity.CycleCloseChecklistItem;
import cm.ftg.tontine.president.cycleclose.enums.ChecklistItemStatus;
import cm.ftg.tontine.president.cycleclose.enums.CycleCloseStatus;
import cm.ftg.tontine.president.cycleclose.repository.CycleCloseChecklistRepository;
import cm.ftg.tontine.president.cycleclose.repository.CycleCloseRepository;
import cm.ftg.tontine.president.security.PresidentAccessChecker;
import cm.ftg.tontine.tontine.entity.Cycle;
import cm.ftg.tontine.tontine.entity.Tontine;
import cm.ftg.tontine.tontine.repository.CycleRepository;
import cm.ftg.tontine.tontine.repository.TontineRepository;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CycleCloseService {

    private static final Map<String, String> DEFAULT_CHECKLIST = new LinkedHashMap<>() {{
        put("audit_validated", "Audit valide");
        put("balance_signed", "Bilan signe");
        put("loans_settled", "Prets soldes");
        put("sanctions_collected", "Sanctions encaissees");
    }};

    private final CycleCloseRepository repository;
    private final CycleCloseChecklistRepository checklistRepository;
    private final TontineRepository tontineRepository;
    private final CycleRepository cycleRepository;
    private final PresidentAccessChecker accessChecker;
    private final AuditService auditService;

    public CycleCloseService(CycleCloseRepository repository,
                             CycleCloseChecklistRepository checklistRepository,
                             TontineRepository tontineRepository,
                             CycleRepository cycleRepository,
                             PresidentAccessChecker accessChecker,
                             AuditService auditService) {
        this.repository = repository;
        this.checklistRepository = checklistRepository;
        this.tontineRepository = tontineRepository;
        this.cycleRepository = cycleRepository;
        this.accessChecker = accessChecker;
        this.auditService = auditService;
    }

    @Transactional
    public CycleCloseDto getOrCreateCurrent(UUID tontineId, UUID userId) {
        accessChecker.requirePresident(userId, tontineId);
        Cycle cycle = resolveCurrentCycle(tontineId);
        CycleClose close = repository.findByTontineIdAndCycleId(tontineId, cycle.getId())
                .orElseGet(() -> createWithDefaults(tontineId, cycle));
        return toDto(close);
    }

    @Transactional
    public CycleCloseDto check(UUID tontineId, UUID userId, String key) {
        accessChecker.requirePresident(userId, tontineId);
        Cycle cycle = resolveCurrentCycle(tontineId);
        CycleClose close = repository.findByTontineIdAndCycleId(tontineId, cycle.getId())
                .orElseGet(() -> createWithDefaults(tontineId, cycle));
        CycleCloseChecklistItem item = checklistRepository
                .findByCycleCloseIdAndItemKey(close.getId(), key)
                .orElseThrow(() -> new ResourceNotFoundException("ChecklistItem", key));
        item.setStatus(ChecklistItemStatus.DONE);
        item.setBlockingReason(null);
        checklistRepository.save(item);
        if (close.getStatus() == CycleCloseStatus.NOT_STARTED) {
            close.setStatus(CycleCloseStatus.IN_PROGRESS);
            repository.save(close);
        }
        auditService.record(userId, "CYCLE_CLOSE_CHECK", "CycleCloseChecklistItem",
                item.getId().toString(), tontineId, "{\"key\":\"" + key + "\"}");
        return toDto(close);
    }

    @Transactional
    public CycleCloseDto sign(UUID tontineId, UUID userId, SignCycleCloseRequest req) {
        accessChecker.requirePresident(userId, tontineId);
        Cycle cycle = resolveCurrentCycle(tontineId);
        CycleClose close = repository.findByTontineIdAndCycleId(tontineId, cycle.getId())
                .orElseGet(() -> createWithDefaults(tontineId, cycle));

        List<CycleCloseChecklistItem> items = checklistRepository
                .findByCycleCloseIdOrderByItemKeyAsc(close.getId());
        boolean allDone = !items.isEmpty()
                && items.stream().allMatch(i -> i.getStatus() == ChecklistItemStatus.DONE);
        if (!allDone) {
            throw new ApiException("CYCLE_CLOSE_CHECKLIST_INCOMPLETE",
                    "Tous les elements de la checklist doivent etre faits pour signer la cloture",
                    HttpStatus.valueOf(422));
        }
        close.setStatus(CycleCloseStatus.PRESIDENT_SIGNED);
        close.setPresidentSignedAt(Instant.now());
        close.setNextCycleStartDate(req.nextCycleStartDate());
        close.setNextCycleDrawMode(req.drawMode());
        CycleClose saved = repository.save(close);
        auditService.record(userId, "CYCLE_CLOSE_SIGN", "CycleClose",
                saved.getId().toString(), tontineId,
                "{\"drawMode\":\"" + req.drawMode().name() + "\"}");
        return toDto(saved);
    }

    private CycleClose createWithDefaults(UUID tontineId, Cycle cycle) {
        CycleClose c = new CycleClose();
        c.setTontineId(tontineId);
        c.setCycleId(cycle.getId());
        c.setCycleNumber(cycle.getNumber());
        c.setStatus(CycleCloseStatus.NOT_STARTED);
        CycleClose saved = repository.save(c);
        for (Map.Entry<String, String> entry : DEFAULT_CHECKLIST.entrySet()) {
            CycleCloseChecklistItem item = new CycleCloseChecklistItem();
            item.setCycleCloseId(saved.getId());
            item.setItemKey(entry.getKey());
            item.setLabel(entry.getValue());
            item.setStatus(ChecklistItemStatus.PENDING);
            checklistRepository.save(item);
        }
        return saved;
    }

    private Cycle resolveCurrentCycle(UUID tontineId) {
        Tontine tontine = tontineRepository.findById(tontineId)
                .orElseThrow(() -> new ResourceNotFoundException("Tontine", tontineId));
        UUID cycleId = tontine.getCurrentCycleId();
        if (cycleId == null) {
            throw new ApiException("NO_CURRENT_CYCLE",
                    "Aucun cycle actif pour cette tontine", HttpStatus.CONFLICT);
        }
        return cycleRepository.findById(cycleId)
                .orElseThrow(() -> new ResourceNotFoundException("Cycle", cycleId));
    }

    private CycleCloseDto toDto(CycleClose close) {
        List<CycleCloseChecklistItemDto> checklist = checklistRepository
                .findByCycleCloseIdOrderByItemKeyAsc(close.getId()).stream()
                .map(CycleCloseChecklistItemDto::from)
                .toList();
        return CycleCloseDto.from(close, checklist);
    }
}
