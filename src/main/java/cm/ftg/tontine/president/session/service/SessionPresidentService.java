package cm.ftg.tontine.president.session.service;

import cm.ftg.tontine.audit.service.AuditService;
import cm.ftg.tontine.common.exception.ApiException;
import cm.ftg.tontine.common.exception.ResourceNotFoundException;
import cm.ftg.tontine.president.security.PresidentAccessChecker;
import cm.ftg.tontine.realtime.RealtimeEventPublisher;
import cm.ftg.tontine.president.session.dto.AgendaItemDto;
import cm.ftg.tontine.president.session.dto.CloseSessionRequest;
import cm.ftg.tontine.president.session.dto.SessionAttendanceEntryDto;
import cm.ftg.tontine.president.session.dto.SessionDto;
import cm.ftg.tontine.president.session.entity.AgendaItem;
import cm.ftg.tontine.president.session.entity.Session;
import cm.ftg.tontine.president.session.entity.SessionAttendanceEntry;
import cm.ftg.tontine.president.session.enums.AgendaItemStatus;
import cm.ftg.tontine.president.session.enums.SessionStatus;
import cm.ftg.tontine.president.session.repository.AgendaItemRepository;
import cm.ftg.tontine.president.session.repository.SessionAttendanceRepository;
import cm.ftg.tontine.president.session.repository.SessionRepository;
import cm.ftg.tontine.tontine.entity.Tontine;
import cm.ftg.tontine.tontine.repository.TontineRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SessionPresidentService {

    private final SessionRepository sessionRepository;
    private final AgendaItemRepository agendaItemRepository;
    private final SessionAttendanceRepository attendanceRepository;
    private final TontineRepository tontineRepository;
    private final PresidentAccessChecker accessChecker;
    private final AuditService auditService;
    private final RealtimeEventPublisher realtime;

    public SessionPresidentService(SessionRepository sessionRepository,
                                   AgendaItemRepository agendaItemRepository,
                                   SessionAttendanceRepository attendanceRepository,
                                   TontineRepository tontineRepository,
                                   PresidentAccessChecker accessChecker,
                                   AuditService auditService,
                                   RealtimeEventPublisher realtime) {
        this.sessionRepository = sessionRepository;
        this.agendaItemRepository = agendaItemRepository;
        this.attendanceRepository = attendanceRepository;
        this.tontineRepository = tontineRepository;
        this.accessChecker = accessChecker;
        this.auditService = auditService;
        this.realtime = realtime;
    }

    @Transactional(readOnly = true)
    public List<SessionDto> listCurrentCycle(UUID tontineId, UUID userId) {
        accessChecker.requirePresident(userId, tontineId);
        Tontine tontine = tontineRepository.findById(tontineId)
                .orElseThrow(() -> new ResourceNotFoundException("Tontine", tontineId));
        List<Session> sessions = (tontine.getCurrentCycleId() != null)
                ? sessionRepository.findByTontineIdAndCycleIdOrderByNumberAsc(tontineId, tontine.getCurrentCycleId())
                : sessionRepository.findByTontineIdOrderByNumberAsc(tontineId);
        return sessions.stream().map(SessionDto::fromSummary).toList();
    }

    @Transactional(readOnly = true)
    public SessionDto findById(UUID id, UUID tontineId, UUID userId) {
        accessChecker.requirePresident(userId, tontineId);
        Session s = loadInTontine(id, tontineId);
        return buildDetailed(s);
    }

    @Transactional
    public SessionDto open(UUID id, UUID tontineId, UUID userId) {
        accessChecker.requirePresident(userId, tontineId);
        Session s = loadInTontine(id, tontineId);
        if (s.getStatus() != SessionStatus.SCHEDULED) {
            throw new ApiException("SESSION_INVALID_STATE",
                    "La seance ne peut etre ouverte que depuis l'etat SCHEDULED", HttpStatus.CONFLICT);
        }
        s.setStatus(SessionStatus.IN_PROGRESS);
        s.setStartedAt(Instant.now());
        Session saved = sessionRepository.save(s);
        auditService.record(userId, "SESSION_OPEN", "Session", id.toString(), tontineId, null);
        realtime.toSession(tontineId, saved.getId(), "session.opened",
                java.util.Map.of("sessionId", saved.getId(), "startedAt", saved.getStartedAt()));
        return buildDetailed(saved);
    }

    @Transactional
    public SessionDto advanceAgenda(UUID sessionId, UUID agendaId, UUID tontineId, UUID userId) {
        accessChecker.requirePresident(userId, tontineId);
        Session s = loadInTontine(sessionId, tontineId);
        if (s.getStatus() != SessionStatus.IN_PROGRESS) {
            throw new ApiException("SESSION_NOT_IN_PROGRESS",
                    "La seance doit etre en cours pour avancer l'ordre du jour", HttpStatus.CONFLICT);
        }
        List<AgendaItem> agenda = agendaItemRepository.findBySessionIdOrderByOrderIdxAsc(sessionId);
        AgendaItem current = agenda.stream()
                .filter(a -> a.getId().equals(agendaId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("AgendaItem", agendaId));
        current.setStatus(AgendaItemStatus.DONE);
        agendaItemRepository.save(current);
        agenda.stream()
                .filter(a -> a.getOrderIdx() > current.getOrderIdx()
                        && a.getStatus() == AgendaItemStatus.PENDING)
                .findFirst()
                .ifPresent(next -> {
                    next.setStatus(AgendaItemStatus.IN_PROGRESS);
                    agendaItemRepository.save(next);
                });
        auditService.record(userId, "AGENDA_ADVANCE", "AgendaItem", agendaId.toString(),
                tontineId, "{\"sessionId\":\"" + sessionId + "\"}");
        realtime.toSession(tontineId, sessionId, "agenda.advanced",
                java.util.Map.of("sessionId", sessionId, "completedAgendaId", agendaId));
        return buildDetailed(s);
    }

    @Transactional
    public SessionDto signCagnotte(UUID id, UUID tontineId, UUID userId) {
        accessChecker.requirePresident(userId, tontineId);
        Session s = loadInTontine(id, tontineId);
        if (s.getStatus() != SessionStatus.IN_PROGRESS) {
            throw new ApiException("SESSION_NOT_IN_PROGRESS",
                    "La cagnotte ne peut etre signee qu'en cours de seance", HttpStatus.CONFLICT);
        }
        s.setCagnotteSignedByPresident(true);
        Session saved = sessionRepository.save(s);
        auditService.record(userId, "SESSION_SIGN_CAGNOTTE", "Session", id.toString(), tontineId, null);
        realtime.toSession(tontineId, saved.getId(), "cagnotte.signed",
                java.util.Map.of("sessionId", saved.getId(),
                        "beneficiaryMemberId", saved.getBeneficiaryMemberId(),
                        "cagnotteAmount", saved.getCagnotteAmount()));
        return buildDetailed(saved);
    }

    @Transactional
    public SessionDto close(UUID id, UUID tontineId, UUID userId, CloseSessionRequest req) {
        accessChecker.requirePresident(userId, tontineId);
        Session s = loadInTontine(id, tontineId);
        if (s.getStatus() != SessionStatus.IN_PROGRESS) {
            throw new ApiException("SESSION_NOT_IN_PROGRESS",
                    "La seance doit etre en cours pour etre cloturee", HttpStatus.CONFLICT);
        }
        s.setStatus(SessionStatus.COMPLETED);
        s.setEndedAt(Instant.now());
        if (req != null && req.nextSessionDate() != null) {
            s.setNextSessionDate(req.nextSessionDate());
        }
        Session saved = sessionRepository.save(s);
        auditService.record(userId, "SESSION_CLOSE", "Session", id.toString(), tontineId, null);
        realtime.toSession(tontineId, saved.getId(), "session.closed",
                java.util.Map.of("sessionId", saved.getId(), "endedAt", saved.getEndedAt()));
        return buildDetailed(saved);
    }

    private Session loadInTontine(UUID id, UUID tontineId) {
        Session s = sessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Session", id));
        if (!s.getTontineId().equals(tontineId)) {
            throw new ApiException("FORBIDDEN", "Tontine non concordante", HttpStatus.FORBIDDEN);
        }
        return s;
    }

    private SessionDto buildDetailed(Session s) {
        List<AgendaItemDto> agenda = agendaItemRepository.findBySessionIdOrderByOrderIdxAsc(s.getId())
                .stream().map(AgendaItemDto::from).toList();
        List<SessionAttendanceEntryDto> attendance = attendanceRepository.findBySessionIdOrderByFullNameAsc(s.getId())
                .stream().map(SessionAttendanceEntryDto::from).toList();
        return SessionDto.from(s, agenda, attendance);
    }
}
