package cm.ftg.tontine.auditor.control.service;

import cm.ftg.tontine.audit.service.AuditService;
import cm.ftg.tontine.auditor.control.dto.CompleteControlRequest;
import cm.ftg.tontine.auditor.control.dto.ControlCheckpointDto;
import cm.ftg.tontine.auditor.control.dto.ControlDto;
import cm.ftg.tontine.auditor.control.dto.CreateControlRequest;
import cm.ftg.tontine.auditor.control.entity.Control;
import cm.ftg.tontine.auditor.control.entity.ControlCheckpoint;
import cm.ftg.tontine.auditor.control.enums.ControlStatus;
import cm.ftg.tontine.auditor.control.repository.ControlCheckpointRepository;
import cm.ftg.tontine.auditor.control.repository.ControlRepository;
import cm.ftg.tontine.auditor.security.AuditorAccessChecker;
import cm.ftg.tontine.common.exception.ApiException;
import cm.ftg.tontine.common.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ControlService {

    private final ControlRepository controlRepository;
    private final ControlCheckpointRepository checkpointRepository;
    private final AuditorAccessChecker accessChecker;
    private final AuditService auditService;

    public ControlService(ControlRepository controlRepository,
                          ControlCheckpointRepository checkpointRepository,
                          AuditorAccessChecker accessChecker,
                          AuditService auditService) {
        this.controlRepository = controlRepository;
        this.checkpointRepository = checkpointRepository;
        this.accessChecker = accessChecker;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<ControlDto> list(UUID tontineId, UUID userId) {
        accessChecker.requireAuditor(userId, tontineId);
        List<Control> controls = controlRepository.findByTontineIdOrderByCreatedAtDesc(tontineId);
        if (controls.isEmpty()) {
            return List.of();
        }
        List<UUID> ids = controls.stream().map(Control::getId).toList();
        Map<UUID, List<ControlCheckpointDto>> grouped = new HashMap<>();
        for (ControlCheckpoint cp : checkpointRepository.findByControlIdInOrderByOrderIdxAsc(ids)) {
            grouped.computeIfAbsent(cp.getControlId(), k -> new ArrayList<>())
                    .add(ControlCheckpointDto.from(cp));
        }
        return controls.stream()
                .map(c -> ControlDto.from(c, grouped.getOrDefault(c.getId(), List.of())))
                .toList();
    }

    @Transactional
    public ControlDto create(UUID tontineId, UUID userId, String fullName, CreateControlRequest req) {
        accessChecker.requireAuditor(userId, tontineId);
        Control c = new Control();
        c.setTontineId(tontineId);
        c.setKind(req.kind());
        c.setPeriodFrom(req.periodFrom());
        c.setPeriodTo(req.periodTo());
        c.setStatus(ControlStatus.PLANNED);
        c.setCreatedByUserId(userId);
        c.setCreatedByFullName(fullName);
        Control saved = controlRepository.save(c);
        auditService.record(userId, "CONTROL_CREATE", "Control", saved.getId().toString(), tontineId,
                "{\"kind\":\"" + req.kind() + "\"}");
        return ControlDto.from(saved, List.of());
    }

    @Transactional
    public ControlDto complete(UUID id, UUID tontineId, UUID userId, CompleteControlRequest req) {
        accessChecker.requireAuditor(userId, tontineId);
        Control c = controlRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Control", id));
        if (!c.getTontineId().equals(tontineId)) {
            throw new ApiException("FORBIDDEN", "Tontine non concordante", HttpStatus.FORBIDDEN);
        }
        if (c.getStatus() == ControlStatus.COMPLETED) {
            throw new ApiException("CONTROL_ALREADY_COMPLETED",
                    "Le controle est deja finalise", HttpStatus.CONFLICT);
        }

        checkpointRepository.deleteByControlId(id);
        List<ControlCheckpointDto> savedDtos = new ArrayList<>();
        int idx = 0;
        for (CompleteControlRequest.CheckpointInput in : req.checkpoints()) {
            ControlCheckpoint cp = new ControlCheckpoint();
            cp.setControlId(id);
            cp.setLabel(in.label());
            cp.setCategory(in.category());
            cp.setExpectedValue(in.expectedValue());
            cp.setObservedValue(in.observedValue());
            if (in.expectedValue() != null && in.observedValue() != null) {
                BigDecimal variance = in.observedValue().subtract(in.expectedValue());
                cp.setVariance(variance);
                cp.setConform(variance.compareTo(BigDecimal.ZERO) == 0);
            } else {
                cp.setVariance(null);
                cp.setConform(null);
            }
            cp.setNote(in.note());
            cp.setOrderIdx(idx++);
            savedDtos.add(ControlCheckpointDto.from(checkpointRepository.save(cp)));
        }

        c.setStatus(ControlStatus.COMPLETED);
        c.setCompletedAt(Instant.now());
        c.setObservations(req.observations());
        Control saved = controlRepository.save(c);

        auditService.record(userId, "CONTROL_COMPLETE", "Control", id.toString(), tontineId,
                "{\"checkpoints\":" + req.checkpoints().size() + "}");
        return ControlDto.from(saved, savedDtos);
    }
}
