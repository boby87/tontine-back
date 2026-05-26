package cm.ftg.tontine.censor.attendance.service;

import cm.ftg.tontine.audit.service.AuditService;
import cm.ftg.tontine.censor.attendance.dto.AttendanceModificationRequestDto;
import cm.ftg.tontine.censor.attendance.dto.DecideAttendanceModificationRequest;
import cm.ftg.tontine.censor.attendance.entity.AttendanceModificationRequest;
import cm.ftg.tontine.censor.attendance.enums.AttendanceModificationStatus;
import cm.ftg.tontine.censor.attendance.repository.AttendanceModificationRequestRepository;
import cm.ftg.tontine.censor.security.CensorAccessChecker;
import cm.ftg.tontine.common.exception.ApiException;
import cm.ftg.tontine.common.exception.ResourceNotFoundException;
import cm.ftg.tontine.president.sanction.entity.Sanction;
import cm.ftg.tontine.president.sanction.enums.SanctionCancelByRole;
import cm.ftg.tontine.president.sanction.enums.SanctionStatus;
import cm.ftg.tontine.president.sanction.repository.SanctionRepository;
import cm.ftg.tontine.president.session.entity.SessionAttendanceEntry;
import cm.ftg.tontine.president.session.enums.AttendanceStatus;
import cm.ftg.tontine.president.session.repository.SessionAttendanceRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AttendanceModificationCensorService {

    private final AttendanceModificationRequestRepository repository;
    private final SessionAttendanceRepository attendanceRepository;
    private final SanctionRepository sanctionRepository;
    private final CensorAccessChecker accessChecker;
    private final AuditService auditService;

    public AttendanceModificationCensorService(AttendanceModificationRequestRepository repository,
                                               SessionAttendanceRepository attendanceRepository,
                                               SanctionRepository sanctionRepository,
                                               CensorAccessChecker accessChecker,
                                               AuditService auditService) {
        this.repository = repository;
        this.attendanceRepository = attendanceRepository;
        this.sanctionRepository = sanctionRepository;
        this.accessChecker = accessChecker;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<AttendanceModificationRequestDto> list(UUID tontineId, UUID userId) {
        accessChecker.requireCensor(userId, tontineId);
        return repository.findByTontineIdOrderByRequestedAtDesc(tontineId).stream()
                .sorted(Comparator
                        .comparing((AttendanceModificationRequest r) ->
                                r.getStatus() == AttendanceModificationStatus.PENDING ? 0 : 1)
                        .thenComparing(AttendanceModificationRequest::getRequestedAt,
                                Comparator.reverseOrder()))
                .map(AttendanceModificationRequestDto::from)
                .toList();
    }

    @Transactional
    public AttendanceModificationRequestDto decide(UUID id, UUID tontineId, UUID userId,
                                                   DecideAttendanceModificationRequest req) {
        accessChecker.requireCensor(userId, tontineId);
        AttendanceModificationRequest r = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AttendanceModificationRequest", id));
        if (!r.getTontineId().equals(tontineId)) {
            throw new ApiException("FORBIDDEN", "Tontine non concordante", HttpStatus.FORBIDDEN);
        }
        if (r.getStatus() != AttendanceModificationStatus.PENDING
                && r.getStatus() != AttendanceModificationStatus.INFO_REQUESTED) {
            throw new ApiException("ATTENDANCE_MODIFICATION_INVALID_STATE",
                    "Cette demande n'est plus en cours d'examen", HttpStatus.CONFLICT);
        }
        switch (req.decision()) {
            case APPROVE -> applyApprove(r, userId, req.comment(), tontineId, id);
            case REJECT -> applyReject(r, userId, req.comment(), tontineId, id);
            case REQUEST_INFO -> applyRequestInfo(r, userId, req.comment(), req.infoRequest(),
                    tontineId, id);
        }
        AttendanceModificationRequest saved = repository.save(r);
        return AttendanceModificationRequestDto.from(saved);
    }

    private void applyApprove(AttendanceModificationRequest r, UUID userId, String comment,
                              UUID tontineId, UUID id) {
        SessionAttendanceEntry entry = attendanceRepository
                .findBySessionIdAndMemberId(r.getSessionId(), r.getMemberId())
                .orElseGet(() -> {
                    SessionAttendanceEntry e = new SessionAttendanceEntry();
                    e.setSessionId(r.getSessionId());
                    e.setMemberId(r.getMemberId());
                    e.setFullName(r.getMemberFullName() != null ? r.getMemberFullName() : "");
                    return e;
                });
        entry.setStatus(r.getToStatus());
        attendanceRepository.save(entry);

        r.setStatus(AttendanceModificationStatus.APPROVED);
        r.setDecidedAt(Instant.now());
        r.setDecisionComment(comment);

        if (cancelsSanctionTrigger(r.getFromStatus(), r.getToStatus())) {
            sanctionRepository
                    .findFirstByTontineIdAndSessionIdAndMemberIdAndAutoDetectedTrueAndStatus(
                            r.getTontineId(), r.getSessionId(), r.getMemberId(),
                            SanctionStatus.PENDING)
                    .ifPresent(s -> {
                        s.setStatus(SanctionStatus.CANCELLED);
                        s.setCancelledAt(Instant.now());
                        s.setCancelledByUserId(userId);
                        s.setCancelReason("Attendance modification approved");
                        s.setCancelledByRole(SanctionCancelByRole.CENSOR);
                        if (s.isFinancial()) {
                            s.setRefundInitiated(true);
                        }
                        Sanction savedSanction = sanctionRepository.save(s);
                        r.setLinkedSanctionId(savedSanction.getId());
                    });
        }

        auditService.record(userId, "ATTENDANCE_MODIFICATION_APPROVE",
                "AttendanceModificationRequest", id.toString(), tontineId,
                "{\"comment\":\"" + escape(comment) + "\"}");
    }

    private void applyReject(AttendanceModificationRequest r, UUID userId, String comment,
                             UUID tontineId, UUID id) {
        r.setStatus(AttendanceModificationStatus.REJECTED);
        r.setDecidedAt(Instant.now());
        r.setDecisionComment(comment);
        auditService.record(userId, "ATTENDANCE_MODIFICATION_REJECT",
                "AttendanceModificationRequest", id.toString(), tontineId,
                "{\"comment\":\"" + escape(comment) + "\"}");
    }

    private void applyRequestInfo(AttendanceModificationRequest r, UUID userId, String comment,
                                  String infoRequest, UUID tontineId, UUID id) {
        r.setStatus(AttendanceModificationStatus.INFO_REQUESTED);
        r.setDecidedAt(Instant.now());
        r.setDecisionComment(comment);
        r.setInfoRequest(infoRequest);
        auditService.record(userId, "ATTENDANCE_MODIFICATION_REQUEST_INFO",
                "AttendanceModificationRequest", id.toString(), tontineId,
                "{\"comment\":\"" + escape(comment) + "\"}");
    }

    private boolean cancelsSanctionTrigger(AttendanceStatus from, AttendanceStatus to) {
        boolean fromTriggers = from == AttendanceStatus.ABSENT || from == AttendanceStatus.LATE;
        boolean toTriggers = to == AttendanceStatus.ABSENT || to == AttendanceStatus.LATE;
        return fromTriggers && !toTriggers;
    }

    private String escape(String s) {
        return s == null ? "" : s.replace("\"", "\\\"");
    }
}
