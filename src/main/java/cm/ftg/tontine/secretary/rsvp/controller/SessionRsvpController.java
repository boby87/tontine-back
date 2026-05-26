package cm.ftg.tontine.secretary.rsvp.controller;

import cm.ftg.tontine.common.dto.ApiResponse;
import cm.ftg.tontine.president.common.TontineIdResolver;
import cm.ftg.tontine.secretary.rsvp.dto.RemindResultDto;
import cm.ftg.tontine.secretary.rsvp.dto.SessionRsvpDto;
import cm.ftg.tontine.secretary.rsvp.dto.SessionRsvpSummaryDto;
import cm.ftg.tontine.secretary.rsvp.dto.UpsertRsvpRequest;
import cm.ftg.tontine.secretary.rsvp.service.SessionRsvpService;
import cm.ftg.tontine.security.AuthenticatedUser;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/secretary/sessions")
public class SessionRsvpController {

    private final SessionRsvpService service;
    private final TontineIdResolver tontineIdResolver;

    public SessionRsvpController(SessionRsvpService service, TontineIdResolver tontineIdResolver) {
        this.service = service;
        this.tontineIdResolver = tontineIdResolver;
    }

    @GetMapping("/{sessionId}/rsvps")
    public ApiResponse<SessionRsvpSummaryDto> summary(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID sessionId,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.summary(sessionId, t, user.id()));
    }

    @PostMapping("/{sessionId}/rsvps/{memberId}")
    public ApiResponse<SessionRsvpDto> upsert(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID sessionId,
            @PathVariable UUID memberId,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId,
            @Valid @RequestBody UpsertRsvpRequest req) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.upsert(sessionId, memberId, t, user.id(), req),
                "RSVP enregistre");
    }

    @PostMapping("/{sessionId}/rsvps/remind")
    public ApiResponse<RemindResultDto> remind(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID sessionId,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.remind(sessionId, t, user.id()), "Relances envoyees");
    }
}
