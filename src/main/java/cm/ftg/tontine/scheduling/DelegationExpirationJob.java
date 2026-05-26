package cm.ftg.tontine.scheduling;

import cm.ftg.tontine.audit.service.AuditService;
import cm.ftg.tontine.notification.enums.NotificationCategory;
import cm.ftg.tontine.notification.enums.NotificationKind;
import cm.ftg.tontine.notification.service.NotificationService;
import cm.ftg.tontine.president.delegation.entity.Delegation;
import cm.ftg.tontine.president.delegation.enums.DelegationStatus;
import cm.ftg.tontine.president.delegation.repository.DelegationRepository;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "app.scheduling.delegation-expiration.enabled", havingValue = "true", matchIfMissing = true)
public class DelegationExpirationJob {

    private static final Logger log = LoggerFactory.getLogger(DelegationExpirationJob.class);

    private final DelegationRepository repository;
    private final NotificationService notificationService;
    private final AuditService auditService;

    public DelegationExpirationJob(DelegationRepository repository,
                                   NotificationService notificationService,
                                   AuditService auditService) {
        this.repository = repository;
        this.notificationService = notificationService;
        this.auditService = auditService;
    }

    @Scheduled(cron = "${app.scheduling.delegation-expiration.cron:0 5 * * * *}")
    @Transactional
    public void expireDelegations() {
        Instant now = Instant.now();
        List<Delegation> overdue = repository.findByStatusAndEndsAtBefore(DelegationStatus.ACTIVE, now);
        if (overdue.isEmpty()) {
            return;
        }
        for (Delegation d : overdue) {
            d.setStatus(DelegationStatus.EXPIRED);
            repository.save(d);
            auditService.record(null, "DELEGATION_AUTO_EXPIRE", "Delegation",
                    d.getId().toString(), d.getTontineId(), null);
            notificationService.publish(
                    d.getDelegateeUserId(),
                    d.getTontineId(),
                    NotificationKind.INFO,
                    NotificationCategory.GENERAL,
                    "Delegation expiree",
                    "Votre delegation \"" + truncatedReason(d.getReason()) + "\" est arrivee a echeance.",
                    "/president/delegations/" + d.getId());
        }
        log.info("[DelegationExpirationJob] {} delegations passees en EXPIRED", overdue.size());
    }

    private String truncatedReason(String reason) {
        if (reason == null) {
            return "(sans motif)";
        }
        return reason.length() <= 80 ? reason : reason.substring(0, 77) + "...";
    }
}
