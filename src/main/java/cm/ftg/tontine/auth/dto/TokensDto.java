package cm.ftg.tontine.auth.dto;

public record TokensDto(
        String accessToken,
        String refreshToken,
        long expiresIn
) {
}
