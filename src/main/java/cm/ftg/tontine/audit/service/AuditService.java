package cm.ftg.tontine.audit.service;

import cm.ftg.tontine.audit.entity.AuditLog;
import cm.ftg.tontine.audit.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private final AuditLogRepository repository;
    private final ObjectProvider<HttpServletRequest> requestProvider;

    public AuditService(AuditLogRepository repository, ObjectProvider<HttpServletRequest> requestProvider) {
        this.repository = repository;
        this.requestProvider = requestProvider;
    }

    public void record(UUID actorUserId, String action, String targetType, String targetId,
                       UUID tontineId, String payload) {
        AuditLog log = new AuditLog();
        log.setActorUserId(actorUserId);
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setTontineId(tontineId);
        log.setPayload(payload);
        HttpServletRequest req = requestProvider.getIfAvailable();
        if (req != null) {
            log.setIpAddress(extractClientIp(req));
        }
        repository.save(log);
    }

    private String extractClientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }
}
