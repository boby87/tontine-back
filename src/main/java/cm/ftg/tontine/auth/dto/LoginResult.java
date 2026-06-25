package cm.ftg.tontine.auth.dto;

public sealed interface LoginResult
        permits LoginResult.Authenticated, LoginResult.PendingVerification, LoginResult.TotpRequired {

    record Authenticated(AuthSessionDto session) implements LoginResult {}

    record PendingVerification(String identifier) implements LoginResult {}

    record TotpRequired(TotpChallengeDto challenge) implements LoginResult {}
}
