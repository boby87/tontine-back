package cm.ftg.tontine.controller;

import cm.ftg.tontine.service.SessionResult;
import org.springframework.stereotype.Component;

@Component
public class SessionMapper {

    public SessionResponse toResponse(SessionResult result) {
        return new SessionResponse(
            result.sessionId(),
            result.tontineId(),
            result.sessionNumber(),
            result.scheduledDate(),
            result.startTime(),
            result.endTimePlanned(),
            result.locationAddress(),
            result.agenda(),
            result.status() != null ? result.status().name() : null,
            result.createdByUserId(),
            result.createdAt()
        );
    }
}
