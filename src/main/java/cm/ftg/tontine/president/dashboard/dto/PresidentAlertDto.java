package cm.ftg.tontine.president.dashboard.dto;

import java.util.UUID;

public record PresidentAlertDto(
        UUID id,
        String level,
        String message,
        String link
) {

    public static PresidentAlertDto critical(UUID id, String message, String link) {
        return new PresidentAlertDto(id, "CRITICAL", message, link);
    }

    public static PresidentAlertDto warning(UUID id, String message, String link) {
        return new PresidentAlertDto(id, "WARNING", message, link);
    }
}
