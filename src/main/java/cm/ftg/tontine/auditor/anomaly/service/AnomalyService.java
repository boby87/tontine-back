package cm.ftg.tontine.auditor.anomaly.service;

import cm.ftg.tontine.audit.service.AuditService;
import cm.ftg.tontine.auditor.anomaly.dto.AnomalyDto;
import cm.ftg.tontine.auditor.anomaly.dto.CloseAnomalyRequest;
import cm.ftg.tontine.auditor.anomaly.dto.CreateAnomalyRequest;
import cm.ftg.tontine.auditor.anomaly.entity.Anomaly;
import cm.ftg.tontine.auditor.anomaly.enums.AnomalyStatus;
import cm.ftg.tontine.auditor.anomaly.repository.AnomalyRepository;
import cm.ftg.tontine.auditor.security.AuditorAccessChecker;
import cm.ftg.tontine.common.exception.ApiException;
import cm.ftg.tontine.common.exception.ResourceNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnomalyService {

    private final AnomalyRepository repository;
    private final AuditorAccessChecker accessChecker;
    private final AuditService auditService;

    public AnomalyService(AnomalyRepository repository,
                          AuditorAccessChecker accessChecker,
                          AuditService auditService) {
        this.repository = repository;
        this.accessChecker = accessChecker;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<AnomalyDto> list(UUID tontineId, UUID userId) {
        accessChecker.requireAuditor(userId, tontineId);
        return repository.findByTontineIdOrderByReportedAtDesc(tontineId).stream()
                .map(AnomalyDto::from)
                .toList();
    }

    @Transactional
    public AnomalyDto create(UUID tontineId, UUID userId, String fullName, CreateAnomalyRequest req) {
        accessChecker.requireAuditor(userId, tontineId);
        Anomaly a = new Anomaly();
        a.setTontineId(tontineId);
        a.setCategory(req.category());
        a.setSeverity(req.severity());
        a.setTitle(req.title());
        a.setDescription(req.description());
        a.setAudience(req.audience());
        a.setStatus(AnomalyStatus.OPEN);
        a.setRequestsResponse(Boolean.TRUE.equals(req.requestsResponse()));
        a.setCopyToTreasurer(Boolean.TRUE.equals(req.copyToTreasurer()));
        a.setReportedByUserId(userId);
        a.setReportedByFullName(fullName);
        Anomaly saved = repository.save(a);
        auditService.record(userId, "ANOMALY_CREATE", "Anomaly", saved.getId().toString(), tontineId,
                "{\"category\":\"" + req.category() + "\",\"severity\":\"" + req.severity() + "\"}");
        return AnomalyDto.from(saved);
    }

    @Transactional
    public AnomalyDto close(UUID id, UUID tontineId, UUID userId, CloseAnomalyRequest req) {
        accessChecker.requireAuditor(userId, tontineId);
        Anomaly a = loadInTontine(id, tontineId);
        if (a.getStatus() != AnomalyStatus.OPEN
                && a.getStatus() != AnomalyStatus.IN_RESPONSE
                && a.getStatus() != AnomalyStatus.RESOLVED) {
            throw new ApiException("ANOMALY_INVALID_STATE",
                    "L'anomalie ne peut pas etre cloturee depuis son etat courant", HttpStatus.CONFLICT);
        }
        a.setStatus(AnomalyStatus.CLOSED);
        a.setClosedAt(Instant.now());
        if (req != null && req.resolutionComment() != null) {
            a.setResolutionComment(req.resolutionComment());
        }
        Anomaly saved = repository.save(a);
        auditService.record(userId, "ANOMALY_CLOSE", "Anomaly", id.toString(), tontineId, null);
        return AnomalyDto.from(saved);
    }

    @Transactional
    public AnomalyDto reopen(UUID id, UUID tontineId, UUID userId) {
        accessChecker.requireAuditor(userId, tontineId);
        Anomaly a = loadInTontine(id, tontineId);
        if (a.getStatus() != AnomalyStatus.CLOSED && a.getStatus() != AnomalyStatus.RESOLVED) {
            throw new ApiException("ANOMALY_INVALID_STATE",
                    "L'anomalie ne peut etre reouverte que depuis CLOSED ou RESOLVED", HttpStatus.CONFLICT);
        }
        a.setStatus(AnomalyStatus.OPEN);
        a.setClosedAt(null);
        a.setResolvedAt(null);
        Anomaly saved = repository.save(a);
        auditService.record(userId, "ANOMALY_REOPEN", "Anomaly", id.toString(), tontineId, null);
        return AnomalyDto.from(saved);
    }

    private Anomaly loadInTontine(UUID id, UUID tontineId) {
        Anomaly a = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Anomaly", id));
        if (!a.getTontineId().equals(tontineId)) {
            throw new ApiException("FORBIDDEN", "Tontine non concordante", HttpStatus.FORBIDDEN);
        }
        return a;
    }
}
