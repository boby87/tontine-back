package cm.ftg.tontine.secretary.session.service;

import cm.ftg.tontine.audit.service.AuditService;
import cm.ftg.tontine.common.exception.ApiException;
import cm.ftg.tontine.common.exception.ResourceNotFoundException;
import cm.ftg.tontine.president.session.dto.SessionDto;
import cm.ftg.tontine.president.session.entity.Session;
import cm.ftg.tontine.president.session.enums.SessionStatus;
import cm.ftg.tontine.president.session.repository.SessionRepository;
import cm.ftg.tontine.secretary.security.SecretaryAccessChecker;
import cm.ftg.tontine.secretary.session.dto.BulkSessionItem;
import cm.ftg.tontine.secretary.session.dto.CreateBulkSessionsRequest;
import cm.ftg.tontine.secretary.session.dto.CreateCycleRequest;
import cm.ftg.tontine.secretary.session.dto.CreateSessionRequest;
import cm.ftg.tontine.secretary.session.dto.SecretaryCycleDto;
import cm.ftg.tontine.secretary.session.dto.UpdateSessionRequest;
import cm.ftg.tontine.tontine.entity.Cycle;
import cm.ftg.tontine.tontine.repository.CycleRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SecretarySessionService {

    private final SessionRepository sessionRepository;
    private final CycleRepository cycleRepository;
    private final SecretaryAccessChecker accessChecker;
    private final AuditService auditService;

    public SecretarySessionService(SessionRepository sessionRepository,
                                   CycleRepository cycleRepository,
                                   SecretaryAccessChecker accessChecker,
                                   AuditService auditService) {
        this.sessionRepository = sessionRepository;
        this.cycleRepository = cycleRepository;
        this.accessChecker = accessChecker;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<SecretaryCycleDto> listCycles(UUID tontineId, UUID userId) {
        accessChecker.requireSecretary(userId, tontineId);
        return cycleRepository.findByTontineIdOrderByNumberAsc(tontineId)
                .stream()
                .map(SecretaryCycleDto::from)
                .toList();
    }

    @Transactional
    public SecretaryCycleDto createCycle(UUID tontineId, UUID userId, CreateCycleRequest req) {
        accessChecker.requireSecretary(userId, tontineId);
        int nextNumber = cycleRepository.findMaxNumberByTontineId(tontineId)
                .map(n -> n + 1).orElse(1);
        Cycle cycle = new Cycle();
        cycle.setTontineId(tontineId);
        cycle.setNumber(nextNumber);
        cycle.setStartDate(req.startDate());
        cycle.setActive(false);
        Cycle saved = cycleRepository.save(cycle);
        auditService.record(userId, "CYCLE_CREATE", "Cycle", saved.getId().toString(), tontineId,
                "{\"number\":" + nextNumber + "}");
        return SecretaryCycleDto.from(saved);
    }

    @Transactional(readOnly = true)
    public List<SessionDto> listSessions(UUID tontineId, UUID cycleId, UUID userId) {
        accessChecker.requireSecretary(userId, tontineId);
        validateCycleBelongsToTontine(cycleId, tontineId);
        return sessionRepository.findByTontineIdAndCycleIdOrderByNumberAsc(tontineId, cycleId)
                .stream()
                .map(SessionDto::fromSummary)
                .toList();
    }

    @Transactional
    public SessionDto createSession(UUID tontineId, UUID userId, CreateSessionRequest req) {
        accessChecker.requireSecretary(userId, tontineId);
        Cycle cycle = validateCycleBelongsToTontine(req.cycleId(), tontineId);
        int nextNumber = sessionRepository.findMaxNumberByCycleId(req.cycleId())
                .map(n -> n + 1).orElse(1);
        Session s = buildSession(tontineId, req.cycleId(), nextNumber, req.scheduledAt(), req.location());
        Session saved = sessionRepository.save(s);
        cycle.setTotalSessions(cycle.getTotalSessions() + 1);
        cycleRepository.save(cycle);
        auditService.record(userId, "SESSION_CREATE", "Session", saved.getId().toString(), tontineId,
                "{\"cycleId\":\"" + req.cycleId() + "\",\"number\":" + nextNumber + "}");
        return SessionDto.fromSummary(saved);
    }

    @Transactional
    public List<SessionDto> createBulkSessions(UUID tontineId, UUID userId, CreateBulkSessionsRequest req) {
        accessChecker.requireSecretary(userId, tontineId);
        Cycle cycle = validateCycleBelongsToTontine(req.cycleId(), tontineId);
        int startNumber = sessionRepository.findMaxNumberByCycleId(req.cycleId())
                .map(n -> n + 1).orElse(1);
        List<SessionDto> result = new ArrayList<>();
        int num = startNumber;
        for (BulkSessionItem item : req.sessions()) {
            Session s = buildSession(tontineId, req.cycleId(), num++, item.scheduledAt(), item.location());
            result.add(SessionDto.fromSummary(sessionRepository.save(s)));
        }
        cycle.setTotalSessions(cycle.getTotalSessions() + req.sessions().size());
        cycleRepository.save(cycle);
        auditService.record(userId, "SESSIONS_BULK_CREATE", "Cycle", req.cycleId().toString(), tontineId,
                "{\"count\":" + req.sessions().size() + "}");
        return result;
    }

    @Transactional
    public SessionDto updateSession(UUID id, UUID tontineId, UUID userId, UpdateSessionRequest req) {
        accessChecker.requireSecretary(userId, tontineId);
        Session s = sessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Session", id));
        if (!s.getTontineId().equals(tontineId)) {
            throw new ApiException("FORBIDDEN", "Tontine non concordante", HttpStatus.FORBIDDEN);
        }
        if (s.getStatus() != SessionStatus.SCHEDULED) {
            throw new ApiException("SESSION_INVALID_STATE",
                    "Seule une séance SCHEDULED peut être modifiée", HttpStatus.CONFLICT);
        }
        if (req.scheduledAt() != null) s.setScheduledAt(req.scheduledAt());
        if (req.location() != null) s.setLocation(req.location());
        Session saved = sessionRepository.save(s);
        auditService.record(userId, "SESSION_UPDATE", "Session", id.toString(), tontineId, null);
        return SessionDto.fromSummary(saved);
    }

    private Cycle validateCycleBelongsToTontine(UUID cycleId, UUID tontineId) {
        Cycle cycle = cycleRepository.findById(cycleId)
                .orElseThrow(() -> new ResourceNotFoundException("Cycle", cycleId));
        if (!cycle.getTontineId().equals(tontineId)) {
            throw new ApiException("FORBIDDEN", "Cycle hors tontine", HttpStatus.FORBIDDEN);
        }
        return cycle;
    }

    private Session buildSession(UUID tontineId, UUID cycleId, int number,
                                 java.time.Instant scheduledAt, String location) {
        Session s = new Session();
        s.setTontineId(tontineId);
        s.setCycleId(cycleId);
        s.setNumber(number);
        s.setScheduledAt(scheduledAt);
        s.setLocation(location);
        s.setStatus(SessionStatus.SCHEDULED);
        return s;
    }
}
