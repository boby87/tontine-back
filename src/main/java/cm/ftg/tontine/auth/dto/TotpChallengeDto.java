package cm.ftg.tontine.auth.dto;

/** Retourné par POST /auth/login quand l'utilisateur a activé la 2FA TOTP. */
public record TotpChallengeDto(
        String totpPendingToken,
        boolean requiresTotp
) {
    public static TotpChallengeDto of(String pendingToken) {
        return new TotpChallengeDto(pendingToken, true);
    }
}
