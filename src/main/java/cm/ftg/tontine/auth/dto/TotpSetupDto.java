package cm.ftg.tontine.auth.dto;

/** Retourné par POST /auth/totp/setup — données pour scanner le QR code. */
public record TotpSetupDto(
        String secret,
        String otpauthUri,
        String qrCodeBase64
) {}
