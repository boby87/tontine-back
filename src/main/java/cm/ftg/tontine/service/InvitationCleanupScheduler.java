package cm.ftg.tontine.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Tâche planifiée de nettoyage des invitations expirées.
 * <p>
 * Exécutée toutes les heures sur un Virtual Thread (grâce à
 * {@code spring.threads.virtual.enabled=true} et au scheduler
 * configuré avec {@link java.util.concurrent.Executors#newVirtualThreadPerTaskExecutor()}).
 */
@Component
public class InvitationCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(InvitationCleanupScheduler.class);

    private final TontineInvitationService invitationService;

    public InvitationCleanupScheduler(TontineInvitationService invitationService) {
        this.invitationService = invitationService;
    }

    /**
     * Purge les invitations expirées toutes les heures.
     */
    @Scheduled(fixedRate = 3600_000, initialDelay = 60_000)
    public void nettoyerInvitationsExpirees() {
        log.debug("Début nettoyage invitations expirées — VirtualThread={}",
            Thread.currentThread().isVirtual());

        int supprimees = invitationService.supprimerInvitationsExpirees();

        log.debug("Fin nettoyage invitations : {} supprimée(s)", supprimees);
    }
}

