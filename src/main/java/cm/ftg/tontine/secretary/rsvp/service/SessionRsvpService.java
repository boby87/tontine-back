package cm.ftg.tontine.secretary.rsvp.service;

import cm.ftg.tontine.audit.service.AuditService;
import cm.ftg.tontine.common.exception.ApiException;
import cm.ftg.tontine.common.exception.ResourceNotFoundException;
import cm.ftg.tontine.member.entity.Member;
import cm.ftg.tontine.member.repository.MemberRepository;
import cm.ftg.tontine.president.session.entity.Session;
import cm.ftg.tontine.president.session.repository.SessionRepository;
import cm.ftg.tontine.secretary.rsvp.dto.RemindResultDto;
import cm.ftg.tontine.secretary.rsvp.dto.SessionRsvpDto;
import cm.ftg.tontine.secretary.rsvp.dto.SessionRsvpSummaryDto;
import cm.ftg.tontine.secretary.rsvp.dto.UpsertRsvpRequest;
import cm.ftg.tontine.secretary.rsvp.entity.SessionRsvp;
import cm.ftg.tontine.secretary.rsvp.enums.RsvpStatus;
import cm.ftg.tontine.secretary.rsvp.repository.SessionRsvpRepository;
import cm.ftg.tontine.secretary.security.SecretaryAccessChecker;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SessionRsvpService {

    private final SessionRsvpRepository rsvpRepository;
    private final SessionRepository sessionRepository;
    private final MemberRepository memberRepository;
    private final SecretaryAccessChecker accessChecker;
    private final AuditService auditService;

    public SessionRsvpService(SessionRsvpRepository rsvpRepository,
                              SessionRepository sessionRepository,
                              MemberRepository memberRepository,
                              SecretaryAccessChecker accessChecker,
                              AuditService auditService) {
        this.rsvpRepository = rsvpRepository;
        this.sessionRepository = sessionRepository;
        this.memberRepository = memberRepository;
        this.accessChecker = accessChecker;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public SessionRsvpSummaryDto summary(UUID sessionId, UUID tontineId, UUID userId) {
        accessChecker.requireSecretary(userId, tontineId);
        Session session = loadSession(sessionId, tontineId);

        List<Member> members = memberRepository.findByTontineId(tontineId);
        List<SessionRsvp> existing = rsvpRepository.findBySessionId(sessionId);
        Map<UUID, SessionRsvp> byMember = new HashMap<>();
        for (SessionRsvp r : existing) {
            byMember.put(r.getMemberId(), r);
        }

        List<SessionRsvpDto> rsvps = new ArrayList<>(members.size());
        int confirmed = 0, declined = 0, tentative = 0, pending = 0;
        for (Member m : members) {
            SessionRsvp r = byMember.get(m.getId());
            if (r == null) {
                rsvps.add(SessionRsvpDto.pendingFor(sessionId, m.getId(),
                        m.getFirstName() + " " + m.getLastName()));
                pending++;
            } else {
                rsvps.add(SessionRsvpDto.from(r));
                switch (r.getStatus()) {
                    case CONFIRMED -> confirmed++;
                    case DECLINED -> declined++;
                    case TENTATIVE -> tentative++;
                    case PENDING -> pending++;
                }
            }
        }

        int totalMembers = members.size();
        BigDecimal quorumPercent = session.getQuorumThreshold() != null
                ? session.getQuorumThreshold()
                : BigDecimal.ZERO;
        boolean quorumReached = false;
        if (totalMembers > 0) {
            BigDecimal ratio = BigDecimal.valueOf(confirmed)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalMembers), 2, RoundingMode.HALF_UP);
            quorumReached = ratio.compareTo(quorumPercent) >= 0;
        }

        return new SessionRsvpSummaryDto(
                session.getId(),
                session.getNumber(),
                session.getScheduledAt(),
                totalMembers,
                confirmed,
                declined,
                tentative,
                pending,
                quorumPercent,
                quorumReached,
                rsvps);
    }

    @Transactional
    public SessionRsvpDto upsert(UUID sessionId, UUID memberId, UUID tontineId, UUID userId,
                                 UpsertRsvpRequest req) {
        accessChecker.requireSecretary(userId, tontineId);
        Session session = loadSession(sessionId, tontineId);
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member", memberId));
        if (!member.getTontineId().equals(tontineId)) {
            throw new ApiException("FORBIDDEN", "Membre hors tontine", HttpStatus.FORBIDDEN);
        }

        SessionRsvp rsvp = rsvpRepository.findBySessionIdAndMemberId(sessionId, memberId)
                .orElseGet(() -> {
                    SessionRsvp fresh = new SessionRsvp();
                    fresh.setSessionId(session.getId());
                    fresh.setMemberId(member.getId());
                    fresh.setMemberFullName(member.getFirstName() + " " + member.getLastName());
                    return fresh;
                });
        rsvp.setStatus(req.status());
        rsvp.setReason(req.reason());
        rsvp.setRespondedAt(Instant.now());
        SessionRsvp saved = rsvpRepository.save(rsvp);

        auditService.record(userId, "RSVP_UPSERT", "SessionRsvp",
                saved.getId().toString(), tontineId,
                "{\"sessionId\":\"" + sessionId + "\",\"memberId\":\"" + memberId
                        + "\",\"status\":\"" + req.status().name() + "\"}");
        return SessionRsvpDto.from(saved);
    }

    @Transactional
    public RemindResultDto remind(UUID sessionId, UUID tontineId, UUID userId) {
        accessChecker.requireSecretary(userId, tontineId);
        Session session = loadSession(sessionId, tontineId);

        List<Member> members = memberRepository.findByTontineId(tontineId);
        List<SessionRsvp> existing = rsvpRepository.findBySessionId(sessionId);
        Map<UUID, SessionRsvp> byMember = new HashMap<>();
        for (SessionRsvp r : existing) {
            byMember.put(r.getMemberId(), r);
        }

        int reminded = 0;
        for (Member m : members) {
            SessionRsvp r = byMember.get(m.getId());
            if (r == null || r.getStatus() == RsvpStatus.PENDING) {
                reminded++;
            }
        }

        auditService.record(userId, "RSVP_REMIND", "Session",
                session.getId().toString(), tontineId,
                "{\"remindedCount\":" + reminded + "}");
        return new RemindResultDto(reminded);
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
