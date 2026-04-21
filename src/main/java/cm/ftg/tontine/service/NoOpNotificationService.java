package cm.ftg.tontine.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NoOpNotificationService implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NoOpNotificationService.class);

    @Override
    public void notifierCotisationRecue(ContributionResult result) {
        log.debug("Notification cotisation reçue (no-op) : {}", result.cleIdempotence());
    }

    @Override
    public void notifierAmendeCalculee(ContributionResult result) {
        log.debug("Notification amende calculée (no-op) : {}", result.cleIdempotence());
    }

    @Override
    public void notifierTontineCreee(TontineResult result) {
        log.debug("Notification tontine créée (no-op) : id={}", result.tontineId());
    }

    @Override
    public void notifierSessionPlanifiee(SessionResult result) {
        log.debug("Notification session planifiée (no-op) : id={}", result.sessionId());
    }

    @Override
    public void notifierInscription(UserResult result) {
        log.debug("Notification inscription (no-op) : email={}", result.email());
    }

    @Override
    public void notifierSanctionAppliquee(SanctionResult result) {
        log.debug("Notification sanction appliquée (no-op) : id={}, membre={}",
            result.sanctionId(), result.memberId());
    }

    @Override
    public void notifierInvitationMembre(AddMemberResult result, String tontineNom) {
        log.debug("Notification invitation membre (no-op) : userId={}, tontine={}",
            result.userId(), tontineNom);
    }
}
