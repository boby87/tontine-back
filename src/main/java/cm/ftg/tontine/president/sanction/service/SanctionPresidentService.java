package cm.ftg.tontine.president.sanction.service;

import cm.ftg.tontine.audit.service.AuditService;
import cm.ftg.tontine.common.exception.ApiException;
import cm.ftg.tontine.common.exception.ResourceNotFoundException;
import cm.ftg.tontine.member.entity.Member;
import cm.ftg.tontine.member.repository.MemberRepository;
import cm.ftg.tontine.notification.enums.NotificationCategory;
import cm.ftg.tontine.notification.enums.NotificationKind;
import cm.ftg.tontine.notification.service.NotificationService;
import cm.ftg.tontine.president.sanction.dto.SanctionDto;
import cm.ftg.tontine.president.sanction.dto.WaiveSanctionRequest;
import cm.ftg.tontine.president.sanction.entity.Sanction;
import cm.ftg.tontine.president.sanction.enums.SanctionCancelByRole;
import cm.ftg.tontine.president.sanction.enums.SanctionStatus;
import cm.ftg.tontine.president.sanction.repository.SanctionRepository;
import cm.ftg.tontine.president.security.PresidentAccessChecker;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SanctionPresidentService {

    private final SanctionRepository repository;
    private final PresidentAccessChecker accessChecker;
    private final AuditService auditService;
    private final MemberRepository memberRepository;
    private final NotificationService notificationService;

    public SanctionPresidentService(SanctionRepository repository,
                                    PresidentAccessChecker accessChecker,
                                    AuditService auditService,
                                    MemberRepository memberRepository,
                                    NotificationService notificationService) {
        this.repository = repository;
        this.accessChecker = accessChecker;
        this.auditService = auditService;
        this.memberRepository = memberRepository;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<SanctionDto> listForReview(UUID tontineId, UUID userId) {
        accessChecker.requirePresident(userId, tontineId);
        return repository.findByTontineIdAndStatusInOrderByIssuedAtDesc(tontineId,
                        List.of(SanctionStatus.CONTESTED, SanctionStatus.PENDING)).stream()
                .map(SanctionDto::from)
                .toList();
    }

    @Transactional
    public SanctionDto waive(UUID id, UUID tontineId, UUID userId, String fullName, WaiveSanctionRequest req) {
        accessChecker.requirePresident(userId, tontineId);
        Sanction s = loadInTontine(id, tontineId);
        ensureTransitionable(s);
        s.setStatus(SanctionStatus.WAIVED);
        s.setCancelledAt(Instant.now());
        s.setCancelledByUserId(userId);
        s.setCancelledByFullName(fullName);
        s.setCancelReason(req.reason());
        s.setCancelledByRole(SanctionCancelByRole.PRESIDENT);
        if (s.isFinancial()) {
            s.setRefundInitiated(true);
        }
        Sanction saved = repository.save(s);
        auditService.record(userId, "SANCTION_WAIVE", "Sanction", id.toString(), tontineId,
                "{\"reason\":\"" + escape(req.reason()) + "\"}");
        notifyMember(saved, tontineId, NotificationKind.SUCCESS,
                "Sanction levee",
                "Votre sanction (" + saved.getType().name() + ") a ete levee par le President"
                        + (req.reason() != null && !req.reason().isBlank() ? " — " + req.reason() : ""));
        return SanctionDto.from(saved);
    }

    @Transactional
    public SanctionDto confirm(UUID id, UUID tontineId, UUID userId) {
        accessChecker.requirePresident(userId, tontineId);
        Sanction s = loadInTontine(id, tontineId);
        if (s.getStatus() != SanctionStatus.PENDING && s.getStatus() != SanctionStatus.CONTESTED) {
            throw new ApiException("SANCTION_INVALID_STATE",
                    "La sanction n'est pas en attente de confirmation", HttpStatus.CONFLICT);
        }
        s.setStatus(SanctionStatus.CONFIRMED);
        s.setResolvedByUserId(userId);
        Sanction saved = repository.save(s);
        auditService.record(userId, "SANCTION_CONFIRM", "Sanction", id.toString(), tontineId, null);
        notifyMember(saved, tontineId, NotificationKind.WARNING,
                "Sanction confirmee",
                "Une sanction (" + saved.getType().name() + ", "
                        + saved.getAmount() + " XAF) a ete confirmee par le President. Motif: "
                        + saved.getReason());
        return SanctionDto.from(saved);
    }

    private void notifyMember(Sanction s, UUID tontineId, NotificationKind kind, String title, String message) {
        UUID userId = memberRepository.findById(s.getMemberId())
                .map(Member::getUserId)
                .orElse(null);
        if (userId != null) {
            notificationService.publish(userId, tontineId, kind,
                    NotificationCategory.SANCTION, title, message,
                    "/members/me/sanctions/" + s.getId());
        }
    }

    private Sanction loadInTontine(UUID id, UUID tontineId) {
        Sanction s = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sanction", id));
        if (!s.getTontineId().equals(tontineId)) {
            throw new ApiException("FORBIDDEN", "Tontine non concordante", HttpStatus.FORBIDDEN);
        }
        return s;
    }

    private void ensureTransitionable(Sanction s) {
        if (s.getStatus() == SanctionStatus.WAIVED || s.getStatus() == SanctionStatus.CANCELLED
                || s.getStatus() == SanctionStatus.PAID) {
            throw new ApiException("SANCTION_FINAL_STATE",
                    "Cette sanction est dans un etat final et ne peut plus etre modifiee",
                    HttpStatus.CONFLICT);
        }
    }

    private String escape(String s) {
        return s == null ? "" : s.replace("\"", "\\\"");
    }
}
