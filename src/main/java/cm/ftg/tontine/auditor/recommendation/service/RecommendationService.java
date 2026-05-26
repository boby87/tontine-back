package cm.ftg.tontine.auditor.recommendation.service;

import cm.ftg.tontine.audit.service.AuditService;
import cm.ftg.tontine.auditor.recommendation.dto.CreateRecommendationRequest;
import cm.ftg.tontine.auditor.recommendation.dto.RecommendationDto;
import cm.ftg.tontine.auditor.recommendation.dto.UpdateRecommendationStatusRequest;
import cm.ftg.tontine.auditor.recommendation.entity.Recommendation;
import cm.ftg.tontine.auditor.recommendation.enums.RecommendationStatus;
import cm.ftg.tontine.auditor.recommendation.repository.RecommendationRepository;
import cm.ftg.tontine.auditor.security.AuditorAccessChecker;
import cm.ftg.tontine.common.exception.ApiException;
import cm.ftg.tontine.common.exception.ResourceNotFoundException;
import cm.ftg.tontine.member.entity.Member;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecommendationService {

    private static final Map<RecommendationStatus, Set<RecommendationStatus>> ALLOWED;
    static {
        ALLOWED = new EnumMap<>(RecommendationStatus.class);
        // skip-forward allowed: PENDING -> IN_PROGRESS | IMPLEMENTED | CLOSED | OVERDUE
        ALLOWED.put(RecommendationStatus.PENDING, EnumSet.of(
                RecommendationStatus.PENDING,
                RecommendationStatus.IN_PROGRESS,
                RecommendationStatus.IMPLEMENTED,
                RecommendationStatus.CLOSED,
                RecommendationStatus.OVERDUE));
        ALLOWED.put(RecommendationStatus.IN_PROGRESS, EnumSet.of(
                RecommendationStatus.IN_PROGRESS,
                RecommendationStatus.IMPLEMENTED,
                RecommendationStatus.CLOSED,
                RecommendationStatus.OVERDUE));
        ALLOWED.put(RecommendationStatus.IMPLEMENTED, EnumSet.of(
                RecommendationStatus.IMPLEMENTED,
                RecommendationStatus.CLOSED,
                RecommendationStatus.OVERDUE));
        ALLOWED.put(RecommendationStatus.CLOSED, EnumSet.of(RecommendationStatus.CLOSED));
        ALLOWED.put(RecommendationStatus.OVERDUE, EnumSet.of(
                RecommendationStatus.OVERDUE,
                RecommendationStatus.IN_PROGRESS,
                RecommendationStatus.IMPLEMENTED,
                RecommendationStatus.CLOSED));
    }

    private final RecommendationRepository repository;
    private final AuditorAccessChecker accessChecker;
    private final AuditService auditService;

    public RecommendationService(RecommendationRepository repository,
                                 AuditorAccessChecker accessChecker,
                                 AuditService auditService) {
        this.repository = repository;
        this.accessChecker = accessChecker;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<RecommendationDto> list(UUID tontineId, UUID userId) {
        accessChecker.requireAuditor(userId, tontineId);
        return repository.findByTontineIdOrderByCreatedAtDesc(tontineId).stream()
                .map(RecommendationDto::from)
                .toList();
    }

    @Transactional
    public RecommendationDto create(UUID tontineId, UUID userId, CreateRecommendationRequest req) {
        Member auditor = accessChecker.requireAuditor(userId, tontineId);
        Recommendation r = new Recommendation();
        r.setTontineId(tontineId);
        r.setOrigin(req.origin());
        r.setOriginId(req.originId());
        r.setTitle(req.title());
        r.setDescription(req.description());
        r.setPriority(req.priority());
        r.setRecipient(req.recipient());
        r.setDueDate(req.dueDate());
        r.setStatus(RecommendationStatus.PENDING);
        r.setProgress(0);
        r.setCreatedByUserId(userId);
        r.setCreatedByFullName(auditor.getFirstName() + " " + auditor.getLastName());
        Recommendation saved = repository.save(r);
        auditService.record(userId, "RECOMMENDATION_CREATE", "Recommendation",
                saved.getId().toString(), tontineId,
                "{\"origin\":\"" + req.origin().name()
                        + "\",\"priority\":\"" + req.priority().name() + "\"}");
        return RecommendationDto.from(saved);
    }

    @Transactional
    public RecommendationDto updateStatus(UUID id, UUID tontineId, UUID userId,
                                          UpdateRecommendationStatusRequest req) {
        accessChecker.requireAuditor(userId, tontineId);
        Recommendation r = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recommendation", id));
        if (!r.getTontineId().equals(tontineId)) {
            throw new ApiException("FORBIDDEN", "Tontine non concordante", HttpStatus.FORBIDDEN);
        }
        RecommendationStatus from = r.getStatus();
        RecommendationStatus to = req.status();
        Set<RecommendationStatus> allowed = ALLOWED.getOrDefault(from, EnumSet.noneOf(RecommendationStatus.class));
        if (!allowed.contains(to)) {
            throw new ApiException("RECOMMENDATION_INVALID_TRANSITION",
                    "Transition de statut invalide: " + from + " -> " + to,
                    HttpStatus.CONFLICT);
        }
        if (to == RecommendationStatus.CLOSED) {
            String note = req.closeNote();
            if (note == null || note.isBlank()) {
                throw new ApiException("RECOMMENDATION_CLOSE_NOTE_REQUIRED",
                        "Note de cloture requise", HttpStatus.valueOf(422));
            }
            r.setCloseNote(note);
        } else if (req.closeNote() != null && !req.closeNote().isBlank()) {
            r.setCloseNote(req.closeNote());
        }
        if (req.progress() != null) {
            int p = Math.max(0, Math.min(100, req.progress()));
            r.setProgress(p);
        }
        r.setStatus(to);
        Recommendation saved = repository.save(r);
        auditService.record(userId, "RECOMMENDATION_STATUS_CHANGE", "Recommendation",
                saved.getId().toString(), tontineId,
                "{\"from\":\"" + from.name() + "\",\"to\":\"" + to.name() + "\"}");
        return RecommendationDto.from(saved);
    }
}
