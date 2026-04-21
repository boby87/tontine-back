package cm.ftg.tontine.service;

public interface NotificationService {
    void notifierCotisationRecue(ContributionResult result);
    void notifierAmendeCalculee(ContributionResult result);
    void notifierTontineCreee(TontineResult result);
    void notifierSessionPlanifiee(SessionResult result);
    void notifierInscription(UserResult result);
    void notifierSanctionAppliquee(SanctionResult result);
    void notifierInvitationMembre(AddMemberResult result, String tontineNom);
}
