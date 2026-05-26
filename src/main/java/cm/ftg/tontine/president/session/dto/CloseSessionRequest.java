package cm.ftg.tontine.president.session.dto;

import java.time.Instant;

public record CloseSessionRequest(
        Instant nextSessionDate
) {
}
