package cm.ftg.tontine.censor.contestation.service;

import cm.ftg.tontine.audit.service.AuditService;
import cm.ftg.tontine.censor.contestation.dto.DecideContestationRequest;
import cm.ftg.tontine.censor.security.CensorAccessChecker;
import cm.ftg.tontine.common.exception.ApiException;
import cm.ftg.tontine.common.exception.ResourceNotFoundException;
import cm.ftg.tontine.president.sanction.dto.SanctionDto;
import cm.ftg.tontine.president.sanction.entity.Sanction;
import cm.ftg.tontine.president.sanction.enums.SanctionCancelByRole;
import cm.ftg.tontine.president.sanction.enums.SanctionStatus;
import cm.ftg.tontine.president.sanction.repository.SanctionRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContestationCensorService {

    private final SanctionRepository sanctionRepository;
    private final CensorAccessChecker accessChecker;
    private final AuditService auditService;

    public ContestationCensorService(SanctionRepository sanctionRepository,
                                     CensorAccessChecker accessChecker,
                                     AuditService auditService) {
        this.sanctionRepository = sanctionRepository;
        this.accessChecker = accessChecker;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<SanctionDto> list(UUID tontineId, UUID userId) {
        accessChecker.requireCensor(userId, tontineId);
        return sanctionRepository
                .findByTontineIdAndStatusOrderByIssuedAtDesc(tontineId, SanctionStatus.CONTESTED)
                .stream()
                .map(SanctionDto::from)
                .toList();
    }

    @Transactional
    public SanctionDto decide(UUID id, UUID tontineId, UUID userId, String fullName,
                              DecideContestationRequest req) {
        accessChecker.requireCensor(userId, tontineId);
        Sanction s = sanctionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sanction", id));
        if (!s.getTontineId().equals(tontineId)) {
            throw new ApiException("FORBIDDEN", "Tontine non concordante", HttpStatus.FORBIDDEN);
        }
        if (s.getStatus() != SanctionStatus.CONTESTED) {
            throw new ApiException("CONTESTATION_INVALID_STATE",
                    "La sanction n'est pas en etat conteste", HttpStatus.CONFLICT);
        }
        switch (req.decision()) {
            case ACCEPT -> applyAccept(s, userId, fullName, req.comment(), tontineId, id);
            case REJECT -> applyReject(s, userId, req.comment(), tontineId, id);
            case TRANSFER_PRESIDENT -> applyTransfer(s, userId, req.comment(), tontineId, id);
        }
        Sanction saved = sanctionRepository.save(s);
        return SanctionDto.from(saved);
    }

    private void applyAccept(Sanction s, UUID userId, String fullName, String comment,
                             UUID tontineId, UUID id) {
        s.setStatus(SanctionStatus.CANCELLED);
        s.setCancelledAt(Instant.now());
        s.setCancelledByUserId(userId);
        s.setCancelledByFullName(fullName);
        s.setCancelReason(comment);
        s.setCancelledByRole(SanctionCancelByRole.CENSOR);
        s.setResolvedByUserId(userId);
        if (s.isFinancial()) {
            s.setRefundInitiated(true);
        }
        auditService.record(userId, "SANCTION_CONTESTATION_ACCEPT", "Sanction", id.toString(),
                tontineId, "{\"comment\":\"" + escape(comment) + "\"}");
    }

    private void applyReject(Sanction s, UUID userId, String comment, UUID tontineId, UUID id) {
        s.setStatus(SanctionStatus.CONFIRMED);
        s.setResolvedByUserId(userId);
        auditService.record(userId, "SANCTION_CONTESTATION_REJECT", "Sanction", id.toString(),
                tontineId, "{\"comment\":\"" + escape(comment) + "\"}");
    }

    private void applyTransfer(Sanction s, UUID userId, String comment, UUID tontineId, UUID id) {
        s.setResolvedByUserId(userId);
        String note = "Escalated to President";
        if (comment != null && !comment.isBlank()) {
            note = note + ": " + comment;
        }
        String existing = s.getCancelReason();
        s.setCancelReason(existing == null || existing.isBlank() ? note : existing + " | " + note);
        auditService.record(userId, "SANCTION_TRANSFER_PRESIDENT", "Sanction", id.toString(),
                tontineId, "{\"comment\":\"" + escape(comment) + "\"}");
    }

    private String escape(String s) {
        return s == null ? "" : s.replace("\"", "\\\"");
    }
}
