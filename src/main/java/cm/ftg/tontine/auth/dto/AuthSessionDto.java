package cm.ftg.tontine.auth.dto;

import java.util.UUID;

public record AuthSessionDto(
        UserDto user,
        TokensDto tokens,
        UUID activeTontineId
) {
}
