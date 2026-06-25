package cm.ftg.tontine.audit.service;

import cm.ftg.tontine.audit.entity.AuditLog;
import cm.ftg.tontine.audit.repository.AuditLogRepository;
import cm.ftg.tontine.common.enums.UserRole;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private final AuditLogRepository repository;
    private final ObjectProvider<HttpServletRequest> requestProvider;

    public AuditService(AuditLogRepository repository,
                        ObjectProvider<HttpServletRequest> requestProvider) {
        this.repository = repository;
        this.requestProvider = requestProvider;
    }

    /** Enregistre une action sans valeurs avant/après (opérations simples). */
    public void record(UUID performedBy, String action, String entityType, String entityId,
                       UUID tontineId, String newValues) {
        record(performedBy, null, action, entityType, entityId, tontineId, null, newValues);
    }

    /** Enregistre une action avec état avant et après (opérations de modification). */
    public void record(UUID performedBy, UserRole performedByRole,
                       String action, String entityType, String entityId,
                       UUID tontineId, String oldValues, String newValues) {
        AuditLog log = new AuditLog();
        log.setPerformedBy(performedBy);
        log.setPerformedByRole(performedByRole);
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setTontineId(tontineId);
        log.setOldValues(oldValues);
        log.setNewValues(newValues);

        HttpServletRequest req = requestProvider.getIfAvailable();
        if (req != null) {
            log.setIpAddress(extractClientIp(req));
            log.setUserAgent(req.getHeader("User-Agent"));
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
