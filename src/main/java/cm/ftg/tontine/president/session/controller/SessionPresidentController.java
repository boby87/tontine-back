package cm.ftg.tontine.president.session.controller;

import cm.ftg.tontine.common.dto.ApiResponse;
import cm.ftg.tontine.president.common.TontineIdResolver;
import cm.ftg.tontine.president.session.dto.CloseSessionRequest;
import cm.ftg.tontine.president.session.dto.SessionDto;
import cm.ftg.tontine.president.session.service.SessionPresidentService;
import cm.ftg.tontine.security.AuthenticatedUser;
import java.util.List;
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
@RequestMapping("/president/sessions")
public class SessionPresidentController {

    private final SessionPresidentService service;
    private final TontineIdResolver tontineIdResolver;

    public SessionPresidentController(SessionPresidentService service, TontineIdResolver tontineIdResolver) {
        this.service = service;
        this.tontineIdResolver = tontineIdResolver;
    }

    @GetMapping
    public ApiResponse<List<SessionDto>> list(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.listCurrentCycle(t, user.id()));
    }

    @GetMapping("/{id}")
    public ApiResponse<SessionDto> getById(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.findById(id, t, user.id()));
    }

    @PostMapping("/{id}/open")
    public ApiResponse<SessionDto> open(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.open(id, t, user.id()), "Seance ouverte");
    }

    @PostMapping("/{id}/agenda/{agendaId}/advance")
    public ApiResponse<SessionDto> advanceAgenda(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id,
            @PathVariable UUID agendaId,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.advanceAgenda(id, agendaId, t, user.id()), "Ordre du jour avance");
    }

    @PostMapping("/{id}/sign-cagnotte")
    public ApiResponse<SessionDto> signCagnotte(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.signCagnotte(id, t, user.id()), "Cagnotte signee");
    }

    @PostMapping("/{id}/close")
    public ApiResponse<SessionDto> close(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId,
            @RequestBody(required = false) CloseSessionRequest req) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.close(id, t, user.id(), req), "Seance cloturee");
    }
}
