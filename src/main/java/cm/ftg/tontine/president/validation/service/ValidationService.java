package cm.ftg.tontine.president.validation.service;

import cm.ftg.tontine.audit.service.AuditService;
import cm.ftg.tontine.common.exception.ApiException;
import cm.ftg.tontine.common.exception.ResourceNotFoundException;
import cm.ftg.tontine.notification.enums.NotificationCategory;
import cm.ftg.tontine.notification.enums.NotificationKind;
import cm.ftg.tontine.notification.service.NotificationService;
import cm.ftg.tontine.realtime.RealtimeEventPublisher;
import cm.ftg.tontine.president.security.PresidentAccessChecker;
import cm.ftg.tontine.president.validation.dto.DecisionRequest;
import cm.ftg.tontine.president.validation.dto.ValidationDto;
import cm.ftg.tontine.president.validation.entity.ValidationItem;
import cm.ftg.tontine.president.validation.enums.ValidationCategory;
import cm.ftg.tontine.president.validation.enums.ValidationStatus;
import cm.ftg.tontine.president.validation.repository.ValidationItemRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ValidationService {

    private final ValidationItemRepository repository;
    private final PresidentAccessChecker accessChecker;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final RealtimeEventPublisher realtime;

    public ValidationService(ValidationItemRepository repository,
                             PresidentAccessChecker accessChecker,
                             AuditService auditService,
                             NotificationService notificationService,
                             RealtimeEventPublisher realtime) {
        this.repository = repository;
        this.accessChecker = accessChecker;
        this.auditService = auditService;
        this.notificationService = notificationService;
        this.realtime = realtime;
    }

    @Transactional(readOnly = true)
    public List<ValidationDto> list(UUID tontineId, UUID userId, ValidationCategory category) {
        accessChecker.requirePresident(userId, tontineId);
        List<ValidationItem> items = (category == null)
                ? repository.findByTontineIdAndStatusOrderBySubmittedAtDesc(tontineId, ValidationStatus.PENDING)
                : repository.findByTontineIdAndCategoryAndStatusOrderBySubmittedAtDesc(
                        tontineId, category, ValidationStatus.PENDING);
        return items.stream().map(ValidationDto::from).toList();
    }

    @Transactional(readOnly = true)
    public ValidationDto findById(UUID id, UUID tontineId, UUID userId) {
        accessChecker.requirePresident(userId, tontineId);
        ValidationItem item = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Validation", id));
        if (!item.getTontineId().equals(tontineId)) {
            throw new ApiException("FORBIDDEN", "Tontine non concordante", HttpStatus.FORBIDDEN);
        }
        return ValidationDto.from(item);
    }

    @Transactional
    public ValidationDto decide(UUID id, UUID tontineId, UUID userId, DecisionRequest req) {
        accessChecker.requirePresident(userId, tontineId);
        ValidationItem item = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Validation", id));
        if (!item.getTontineId().equals(tontineId)) {
            throw new ApiException("FORBIDDEN", "Tontine non concordante", HttpStatus.FORBIDDEN);
        }
        if (item.getStatus() != ValidationStatus.PENDING) {
            throw new ApiException("VALIDATION_ALREADY_DECIDED",
                    "Cette validation a deja ete decidee", HttpStatus.CONFLICT);
        }
        item.setDecision(req.decision());
        item.setDecisionComment(req.comment());
        item.setDecidedByUserId(userId);
        item.setDecidedAt(Instant.now());
        item.setStatus(switch (req.decision()) {
            case APPROVED -> ValidationStatus.APPROVED;
            case REJECTED -> ValidationStatus.REJECTED;
            case BLOCKED -> ValidationStatus.BLOCKED;
        });
        ValidationItem saved = repository.save(item);
        auditService.record(userId, "VALIDATION_DECIDE", "Validation", id.toString(),
                tontineId, "{\"decision\":\"" + req.decision().name() + "\"}");

        NotificationKind kind = switch (req.decision()) {
            case APPROVED -> NotificationKind.SUCCESS;
            case REJECTED -> NotificationKind.WARNING;
            case BLOCKED -> NotificationKind.ERROR;
        };
        String verb = switch (req.decision()) {
            case APPROVED -> "approuvee";
            case REJECTED -> "rejetee";
            case BLOCKED -> "bloquee";
        };
        notificationService.publish(
                saved.getSubmittedByUserId(),
                tontineId,
                kind,
                NotificationCategory.VALIDATION,
                "Decision sur votre demande",
                "Votre demande \"" + saved.getTitle() + "\" a ete " + verb
                        + (req.comment() != null && !req.comment().isBlank()
                                ? " — " + req.comment() : ""),
                "/president/validations/" + saved.getId());

        realtime.toPresidentDashboard(tontineId, "validation.decided",
                java.util.Map.of(
                        "validationId", saved.getId(),
                        "category", saved.getCategory().name(),
                        "decision", req.decision().name(),
                        "title", saved.getTitle()));
        return ValidationDto.from(saved);
    }
}
