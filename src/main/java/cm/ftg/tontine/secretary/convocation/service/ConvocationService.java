package cm.ftg.tontine.secretary.convocation.service;

import cm.ftg.tontine.audit.service.AuditService;
import cm.ftg.tontine.common.exception.ApiException;
import cm.ftg.tontine.common.exception.ResourceNotFoundException;
import cm.ftg.tontine.president.session.entity.Session;
import cm.ftg.tontine.president.session.repository.SessionRepository;
import cm.ftg.tontine.secretary.convocation.dto.ConvocationDto;
import cm.ftg.tontine.secretary.convocation.dto.CreateConvocationRequest;
import cm.ftg.tontine.secretary.convocation.dto.CreateReminderRequest;
import cm.ftg.tontine.secretary.convocation.entity.Convocation;
import cm.ftg.tontine.secretary.convocation.entity.ReminderEmbeddable;
import cm.ftg.tontine.secretary.convocation.enums.ConvocationStatus;
import cm.ftg.tontine.secretary.convocation.repository.ConvocationRepository;
import cm.ftg.tontine.secretary.security.SecretaryAccessChecker;
import java.time.Instant;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConvocationService {

    private final ConvocationRepository convocationRepository;
    private final SessionRepository sessionRepository;
    private final SecretaryAccessChecker accessChecker;
    private final AuditService auditService;

    public ConvocationService(ConvocationRepository convocationRepository,
                              SessionRepository sessionRepository,
                              SecretaryAccessChecker accessChecker,
                              AuditService auditService) {
        this.convocationRepository = convocationRepository;
        this.sessionRepository = sessionRepository;
        this.accessChecker = accessChecker;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<ConvocationDto> list(UUID tontineId, UUID userId, UUID sessionId) {
        accessChecker.requireSecretary(userId, tontineId);
        List<Convocation> convocations = sessionId != null
                ? convocationRepository.findByTontineIdAndSessionIdOrderByCreatedAtDesc(tontineId, sessionId)
                : convocationRepository.findByTontineIdOrderByCreatedAtDesc(tontineId);
        return convocations.stream()
                .map(ConvocationDto::from)
                .toList();
    }

    @Transactional
    public ConvocationDto create(UUID tontineId, UUID userId, CreateConvocationRequest req) {
        accessChecker.requireSecretary(userId, tontineId);

        Session session = sessionRepository.findById(req.sessionId())
                .orElseThrow(() -> new ResourceNotFoundException("Session", req.sessionId()));
        if (!session.getTontineId().equals(tontineId)) {
            throw new ApiException("FORBIDDEN", "Seance hors tontine", HttpStatus.FORBIDDEN);
        }

        Convocation c = new Convocation();
        c.setTontineId(tontineId);
        c.setSessionId(session.getId());
        c.setSessionNumber(session.getNumber());
        c.setScheduledFor(session.getScheduledAt());
        c.setMessage(req.message());
        c.setIncludeCandidates(Boolean.TRUE.equals(req.includeCandidates()));
        c.setChannels(EnumSet.copyOf(req.channels()));
        c.setAudienceMemberIds(new HashSet<>(req.audienceMemberIds()));
        c.setTotalRecipients(req.audienceMemberIds().size());
        c.setTotalDelivered(0);
        c.setTotalFailed(0);

        if (req.reminders() != null) {
            for (CreateReminderRequest r : req.reminders()) {
                c.getReminders().add(new ReminderEmbeddable(r.offsetHoursBefore()));
            }
        }

        Instant now = Instant.now();
        if (req.scheduledAt() != null && req.scheduledAt().isAfter(now)) {
            c.setStatus(ConvocationStatus.SCHEDULED);
            c.setScheduledAt(req.scheduledAt());
        } else {
            c.setStatus(ConvocationStatus.SENT);
            c.setSentAt(now);
        }

        Convocation saved = convocationRepository.save(c);
        auditService.record(userId, "CONVOCATION_CREATE", "Convocation",
                saved.getId().toString(), tontineId,
                "{\"sessionId\":\"" + session.getId() + "\",\"status\":\""
                        + saved.getStatus().name() + "\"}");
        return ConvocationDto.from(saved);
    }
}
