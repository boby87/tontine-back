package cm.ftg.tontine.auditor.clarification.service;

import cm.ftg.tontine.audit.service.AuditService;
import cm.ftg.tontine.auditor.clarification.dto.ClarificationDto;
import cm.ftg.tontine.auditor.clarification.dto.CreateClarificationRequest;
import cm.ftg.tontine.auditor.clarification.dto.EvaluateClarificationRequest;
import cm.ftg.tontine.auditor.clarification.dto.SimulateClarificationResponseRequest;
import cm.ftg.tontine.auditor.clarification.entity.Clarification;
import cm.ftg.tontine.auditor.clarification.enums.ClarificationStatus;
import cm.ftg.tontine.auditor.clarification.repository.ClarificationRepository;
import cm.ftg.tontine.auditor.security.AuditorAccessChecker;
import cm.ftg.tontine.common.exception.ApiException;
import cm.ftg.tontine.common.exception.ResourceNotFoundException;
import cm.ftg.tontine.member.entity.Member;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClarificationService {

    private final ClarificationRepository repository;
    private final AuditorAccessChecker accessChecker;
    private final AuditService auditService;

    public ClarificationService(ClarificationRepository repository,
                                AuditorAccessChecker accessChecker,
                                AuditService auditService) {
        this.repository = repository;
        this.accessChecker = accessChecker;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<ClarificationDto> list(UUID tontineId, UUID userId) {
        accessChecker.requireAuditor(userId, tontineId);
        return repository.findByTontineIdOrderByAskedAtDesc(tontineId).stream()
                .map(ClarificationDto::from)
                .toList();
    }

    @Transactional
    public ClarificationDto create(UUID tontineId, UUID userId, CreateClarificationRequest req) {
        Member auditor = accessChecker.requireAuditor(userId, tontineId);
        Clarification c = new Clarification();
        c.setTontineId(tontineId);
        c.setSubject(req.subject());
        c.setQuestion(req.question());
        c.setTargetRole(req.targetRole());
        c.setDueWithinHours(req.dueWithinHours());
        c.setAskedByUserId(userId);
        c.setAskedByFullName(auditor.getFirstName() + " " + auditor.getLastName());
        c.setStatus(ClarificationStatus.PENDING);
        Clarification saved = repository.save(c);
        auditService.record(userId, "CLARIFICATION_ASK", "Clarification",
                saved.getId().toString(), tontineId,
                "{\"targetRole\":\"" + req.targetRole().name() + "\"}");
        return ClarificationDto.from(saved);
    }

    @Transactional
    public ClarificationDto simulateResponse(UUID id, UUID tontineId, UUID userId,
                                             SimulateClarificationResponseRequest req) {
        accessChecker.requireAuditor(userId, tontineId);
        Clarification c = loadAndCheck(id, tontineId);
        if (c.getStatus() != ClarificationStatus.PENDING) {
            throw new ApiException("CLARIFICATION_INVALID_STATE",
                    "La clarification doit etre en statut PENDING", HttpStatus.CONFLICT);
        }
        String response = (req != null && req.response() != null && !req.response().isBlank())
                ? req.response()
                : "(no response provided)";
        c.setResponse(response);
        c.setRespondedAt(Instant.now());
        c.setStatus(ClarificationStatus.RESPONDED);
        Clarification saved = repository.save(c);
        auditService.record(userId, "CLARIFICATION_SIMULATE_RESPONSE", "Clarification",
                saved.getId().toString(), tontineId, null);
        return ClarificationDto.from(saved);
    }

    @Transactional
    public ClarificationDto evaluate(UUID id, UUID tontineId, UUID userId,
                                     EvaluateClarificationRequest req) {
        accessChecker.requireAuditor(userId, tontineId);
        Clarification c = loadAndCheck(id, tontineId);
        if (c.getStatus() != ClarificationStatus.RESPONDED) {
            throw new ApiException("CLARIFICATION_INVALID_STATE",
                    "La clarification doit etre en statut RESPONDED", HttpStatus.CONFLICT);
        }
        c.setEvaluation(req.evaluation());
        c.setEvaluatedAt(Instant.now());
        c.setStatus(ClarificationStatus.EVALUATED);
        Clarification saved = repository.save(c);
        auditService.record(userId, "CLARIFICATION_EVALUATE", "Clarification",
                saved.getId().toString(), tontineId,
                "{\"evaluation\":\"" + req.evaluation().name() + "\"}");
        return ClarificationDto.from(saved);
    }

    private Clarification loadAndCheck(UUID id, UUID tontineId) {
        Clarification c = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Clarification", id));
        if (!c.getTontineId().equals(tontineId)) {
            throw new ApiException("FORBIDDEN", "Tontine non concordante", HttpStatus.FORBIDDEN);
        }
        return c;
    }
}
