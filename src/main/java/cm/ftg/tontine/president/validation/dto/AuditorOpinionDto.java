package cm.ftg.tontine.president.validation.dto;

import cm.ftg.tontine.president.validation.enums.AuditorOpinionStatus;
import java.time.Instant;
import java.util.UUID;

public record AuditorOpinionDto(
        AuditorOpinionStatus status,
        String comment,
        UUID userId,
        String userFullName,
        Instant emittedAt
) {
}
