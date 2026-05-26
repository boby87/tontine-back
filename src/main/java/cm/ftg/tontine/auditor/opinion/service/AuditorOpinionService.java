package cm.ftg.tontine.auditor.opinion.service;

import cm.ftg.tontine.audit.service.AuditService;
import cm.ftg.tontine.auditor.opinion.dto.EmitOpinionRequest;
import cm.ftg.tontine.auditor.security.AuditorAccessChecker;
import cm.ftg.tontine.common.exception.ApiException;
import cm.ftg.tontine.common.exception.ResourceNotFoundException;
import cm.ftg.tontine.member.entity.Member;
import cm.ftg.tontine.president.validation.dto.ValidationDto;
import cm.ftg.tontine.president.validation.entity.ValidationItem;
import cm.ftg.tontine.president.validation.enums.ValidationStatus;
import cm.ftg.tontine.president.validation.repository.ValidationItemRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditorOpinionService {

    private final ValidationItemRepository repository;
    private final AuditorAccessChecker accessChecker;
    private final AuditService auditService;

    public AuditorOpinionService(ValidationItemRepository repository,
                                 AuditorAccessChecker accessChecker,
                                 AuditService auditService) {
        this.repository = repository;
        this.accessChecker = accessChecker;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<ValidationDto> listPending(UUID tontineId, UUID userId) {
        accessChecker.requireAuditor(userId, tontineId);
        return repository.findByTontineIdAndStatusAndAuditorOpinionStatusIsNullOrderBySubmittedAtDesc(
                        tontineId, ValidationStatus.PENDING)
                .stream().map(ValidationDto::from).toList();
    }

    @Transactional
    public ValidationDto emitOpinion(UUID id, UUID tontineId, UUID userId, EmitOpinionRequest req) {
        Member auditor = accessChecker.requireAuditor(userId, tontineId);
        ValidationItem item = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Validation", id));
        if (!item.getTontineId().equals(tontineId)) {
            throw new ApiException("FORBIDDEN", "Tontine non concordante", HttpStatus.FORBIDDEN);
        }
        if (item.getAuditorOpinionStatus() != null) {
            throw new ApiException("AUDITOR_OPINION_ALREADY_EMITTED",
                    "Un avis a deja ete emis sur cette validation", HttpStatus.CONFLICT);
        }
        item.setAuditorOpinionStatus(req.status());
        item.setAuditorOpinionComment(req.comment());
        item.setAuditorUserId(userId);
        item.setAuditorFullName(auditor.getFirstName() + " " + auditor.getLastName());
        item.setAuditorEmittedAt(Instant.now());
        ValidationItem saved = repository.save(item);
        auditService.record(userId, "VALIDATION_OPINION_EMIT", "Validation", id.toString(),
                tontineId, "{\"status\":\"" + req.status().name() + "\"}");
        return ValidationDto.from(saved);
    }
}
