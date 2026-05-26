package cm.ftg.tontine.secretary.attendance.service;

import cm.ftg.tontine.audit.service.AuditService;
import cm.ftg.tontine.common.exception.ApiException;
import cm.ftg.tontine.common.exception.ResourceNotFoundException;
import cm.ftg.tontine.member.entity.Member;
import cm.ftg.tontine.member.repository.MemberRepository;
import cm.ftg.tontine.president.session.dto.SessionAttendanceEntryDto;
import cm.ftg.tontine.president.session.entity.Session;
import cm.ftg.tontine.president.session.entity.SessionAttendanceEntry;
import cm.ftg.tontine.president.session.enums.AttendanceStatus;
import cm.ftg.tontine.president.session.repository.SessionAttendanceRepository;
import cm.ftg.tontine.president.session.repository.SessionRepository;
import cm.ftg.tontine.secretary.attendance.dto.UpsertAttendanceRequest;
import cm.ftg.tontine.secretary.security.SecretaryAccessChecker;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AttendanceSecretaryService {

    private final SessionAttendanceRepository attendanceRepository;
    private final SessionRepository sessionRepository;
    private final MemberRepository memberRepository;
    private final SecretaryAccessChecker accessChecker;
    private final AuditService auditService;

    public AttendanceSecretaryService(SessionAttendanceRepository attendanceRepository,
                                      SessionRepository sessionRepository,
                                      MemberRepository memberRepository,
                                      SecretaryAccessChecker accessChecker,
                                      AuditService auditService) {
        this.attendanceRepository = attendanceRepository;
        this.sessionRepository = sessionRepository;
        this.memberRepository = memberRepository;
        this.accessChecker = accessChecker;
        this.auditService = auditService;
    }

    @Transactional
    public SessionAttendanceEntryDto upsert(UUID sessionId, UUID memberId, UUID tontineId,
                                            UUID userId, UpsertAttendanceRequest req) {
        accessChecker.requireSecretary(userId, tontineId);
        Session session = loadSession(sessionId, tontineId);
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member", memberId));
        if (!member.getTontineId().equals(tontineId)) {
            throw new ApiException("FORBIDDEN", "Membre hors tontine", HttpStatus.FORBIDDEN);
        }

        SessionAttendanceEntry entry = attendanceRepository
                .findBySessionIdAndMemberId(session.getId(), member.getId())
                .orElseGet(() -> {
                    SessionAttendanceEntry fresh = new SessionAttendanceEntry();
                    fresh.setSessionId(session.getId());
                    fresh.setMemberId(member.getId());
                    fresh.setFullName(member.getFirstName() + " " + member.getLastName());
                    return fresh;
                });
        entry.setStatus(req.status());
        if (req.status() == AttendanceStatus.PRESENT || req.status() == AttendanceStatus.LATE) {
            entry.setCheckInAt(Instant.now());
        }
        SessionAttendanceEntry saved = attendanceRepository.save(entry);

        auditService.record(userId, "ATTENDANCE_UPSERT", "SessionAttendanceEntry",
                saved.getId().toString(), tontineId,
                "{\"sessionId\":\"" + sessionId + "\",\"memberId\":\"" + memberId
                        + "\",\"status\":\"" + req.status().name() + "\"}");
        return SessionAttendanceEntryDto.from(saved);
    }

    @Transactional
    public void finalize(UUID sessionId, UUID tontineId, UUID userId) {
        accessChecker.requireSecretary(userId, tontineId);
        Session session = loadSession(sessionId, tontineId);

        List<Member> members = memberRepository.findByTontineId(tontineId);
        List<SessionAttendanceEntry> existing = attendanceRepository.findBySessionId(session.getId());
        Set<UUID> recorded = new HashSet<>();
        for (SessionAttendanceEntry e : existing) {
            recorded.add(e.getMemberId());
        }

        int createdAbsent = 0;
        for (Member m : members) {
            if (!recorded.contains(m.getId())) {
                SessionAttendanceEntry entry = new SessionAttendanceEntry();
                entry.setSessionId(session.getId());
                entry.setMemberId(m.getId());
                entry.setFullName(m.getFirstName() + " " + m.getLastName());
                entry.setStatus(AttendanceStatus.ABSENT);
                attendanceRepository.save(entry);
                createdAbsent++;
            }
        }

        auditService.record(userId, "ATTENDANCE_FINALIZE", "Session",
                session.getId().toString(), tontineId,
                "{\"absentCreated\":" + createdAbsent + "}");
    }

    private Session loadSession(UUID sessionId, UUID tontineId) {
        Session s = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session", sessionId));
        if (!s.getTontineId().equals(tontineId)) {
            throw new ApiException("FORBIDDEN", "Tontine non concordante", HttpStatus.FORBIDDEN);
        }
        return s;
    }
}
