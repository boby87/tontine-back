package cm.ftg.tontine.censor.justification.service;

import cm.ftg.tontine.audit.service.AuditService;
import cm.ftg.tontine.censor.justification.dto.AbsenceJustificationDto;
import cm.ftg.tontine.censor.justification.dto.DecideJustificationRequest;
import cm.ftg.tontine.censor.justification.entity.AbsenceJustification;
import cm.ftg.tontine.censor.justification.enums.JustificationStatus;
import cm.ftg.tontine.censor.justification.repository.AbsenceJustificationRepository;
import cm.ftg.tontine.censor.security.CensorAccessChecker;
import cm.ftg.tontine.common.exception.ApiException;
import cm.ftg.tontine.common.exception.ResourceNotFoundException;
import cm.ftg.tontine.president.sanction.entity.Sanction;
import cm.ftg.tontine.president.sanction.enums.SanctionCancelByRole;
import cm.ftg.tontine.president.sanction.enums.SanctionStatus;
import cm.ftg.tontine.president.sanction.repository.SanctionRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AbsenceJustificationCensorService {

    private static final Set<JustificationStatus> PRIORITY_STATUSES = Set.of(
            JustificationStatus.PENDING_CENSOR,
            JustificationStatus.INFO_REQUESTED);

    private final AbsenceJustificationRepository repository;
    private final SanctionRepository sanctionRepository;
    private final CensorAccessChecker accessChecker;
    private final AuditService auditService;

    public AbsenceJustificationCensorService(AbsenceJustificationRepository repository,
                                             SanctionRepository sanctionRepository,
                                             CensorAccessChecker accessChecker,
                                             AuditService auditService) {
        this.repository = repository;
        this.sanctionRepository = sanctionRepository;
        this.accessChecker = accessChecker;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<AbsenceJustificationDto> list(UUID tontineId, UUID userId) {
        accessChecker.requireCensor(userId, tontineId);
        return repository.findByTontineIdOrderBySubmittedAtDesc(tontineId).stream()
                .sorted(Comparator
                        .comparing((AbsenceJustification j) ->
                                PRIORITY_STATUSES.contains(j.getStatus()) ? 0 : 1)
                        .thenComparing(AbsenceJustification::getSubmittedAt,
                                Comparator.reverseOrder()))
                .map(AbsenceJustificationDto::from)
                .toList();
    }

    @Transactional
    public AbsenceJustificationDto decide(UUID id, UUID tontineId, UUID userId,
                                          DecideJustificationRequest req) {
        accessChecker.requireCensor(userId, tontineId);
        AbsenceJustification j = load(id, tontineId);
        if (j.getStatus() != JustificationStatus.PENDING_CENSOR
                && j.getStatus() != JustificationStatus.INFO_REQUESTED) {
            throw new ApiException("JUSTIFICATION_INVALID_STATE",
                    "Cette justification n'est plus en cours d'examen par le censeur",
                    HttpStatus.CONFLICT);
        }
        switch (req.decision()) {
            case VALIDATE -> {
                j.setStatus(JustificationStatus.PENDING_PRESIDENT);
                j.setCensorDecidedAt(Instant.now());
                j.setCensorComment(req.comment());
                auditService.record(userId, "JUSTIFICATION_VALIDATE_CENSOR",
                        "AbsenceJustification", id.toString(), tontineId,
                        "{\"comment\":\"" + escape(req.comment()) + "\"}");
            }
            case REJECT -> {
                j.setStatus(JustificationStatus.REJECTED_CENSOR);
                j.setCensorDecidedAt(Instant.now());
                j.setCensorComment(req.comment());
                auditService.record(userId, "JUSTIFICATION_REJECT_CENSOR",
                        "AbsenceJustification", id.toString(), tontineId,
                        "{\"comment\":\"" + escape(req.comment()) + "\"}");
            }
            case REQUEST_INFO -> {
                j.setStatus(JustificationStatus.INFO_REQUESTED);
                j.setCensorDecidedAt(Instant.now());
                j.setCensorComment(req.comment());
                auditService.record(userId, "JUSTIFICATION_REQUEST_INFO",
                        "AbsenceJustification", id.toString(), tontineId,
                        "{\"comment\":\"" + escape(req.comment()) + "\"}");
            }
        }
        return AbsenceJustificationDto.from(repository.save(j));
    }

    @Transactional
    public AbsenceJustificationDto presidentApprove(UUID id, UUID tontineId, UUID userId) {
        accessChecker.requireCensor(userId, tontineId);
        AbsenceJustification j = load(id, tontineId);
        if (j.getStatus() != JustificationStatus.PENDING_PRESIDENT) {
            throw new ApiException("JUSTIFICATION_INVALID_STATE",
                    "La justification n'est pas en attente de decision du president",
                    HttpStatus.CONFLICT);
        }
        j.setStatus(JustificationStatus.APPROVED);
        j.setPresidentDecidedAt(Instant.now());
        j.setPresidentComment("Approved (simulated by censor)");

        sanctionRepository
                .findFirstByTontineIdAndSessionIdAndMemberIdAndAutoDetectedTrueAndStatus(
                        j.getTontineId(), j.getSessionId(), j.getMemberId(),
                        SanctionStatus.PENDING)
                .ifPresent(s -> {
                    s.setStatus(SanctionStatus.CANCELLED);
                    s.setCancelledAt(Instant.now());
                    s.setCancelledByUserId(userId);
                    s.setCancelReason("Absence justification approved");
                    s.setCancelledByRole(SanctionCancelByRole.PRESIDENT);
                    if (s.isFinancial()) {
                        s.setRefundInitiated(true);
                    }
                    Sanction saved = sanctionRepository.save(s);
                    j.setLinkedSanctionId(saved.getId());
                });

        auditService.record(userId, "JUSTIFICATION_PRESIDENT_APPROVE",
                "AbsenceJustification", id.toString(), tontineId, null);
        return AbsenceJustificationDto.from(repository.save(j));
    }

    private AbsenceJustification load(UUID id, UUID tontineId) {
        AbsenceJustification j = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AbsenceJustification", id));
        if (!j.getTontineId().equals(tontineId)) {
            throw new ApiException("FORBIDDEN", "Tontine non concordante", HttpStatus.FORBIDDEN);
        }
        return j;
    }

    private String escape(String s) {
        return s == null ? "" : s.replace("\"", "\\\"");
    }
}
