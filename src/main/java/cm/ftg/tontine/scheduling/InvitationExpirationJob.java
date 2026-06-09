package cm.ftg.tontine.scheduling;

import cm.ftg.tontine.audit.service.AuditService;
import cm.ftg.tontine.president.membership.invitation.entity.MembershipInvitation;
import cm.ftg.tontine.president.membership.invitation.enums.InvitationStatus;
import cm.ftg.tontine.president.membership.invitation.repository.MembershipInvitationRepository;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Passe les invitations PENDING/SENT a EXPIRED lorsque leur date d'expiration est depassee.
 */
@Component
@ConditionalOnProperty(name = "app.scheduling.invitation-expiration.enabled", havingValue = "true", matchIfMissing = true)
public class InvitationExpirationJob {

    private static final Logger log = LoggerFactory.getLogger(InvitationExpirationJob.class);

    private final MembershipInvitationRepository invitationRepository;
    private final AuditService auditService;

    public InvitationExpirationJob(MembershipInvitationRepository invitationRepository,
                                   AuditService auditService) {
        this.invitationRepository = invitationRepository;
        this.auditService = auditService;
    }

    @Scheduled(cron = "${app.scheduling.invitation-expiration.cron:0 0 * * * *}")
    @Transactional
    public void expireInvitations() {
        Instant now = Instant.now();
        List<MembershipInvitation> stale = invitationRepository.findByStatusInAndExpiresAtBefore(
                Set.of(InvitationStatus.PENDING, InvitationStatus.SENT), now);
        if (stale.isEmpty()) {
            return;
        }
        for (MembershipInvitation inv : stale) {
            inv.setStatus(InvitationStatus.EXPIRED);
            invitationRepository.save(inv);
            auditService.record(null, "INVITATION_EXPIRE", "MembershipInvitation",
                    inv.getId().toString(), inv.getTontineId(), null);
        }
        log.info("[InvitationExpirationJob] {} invitations passees en EXPIRED", stale.size());
    }
}
