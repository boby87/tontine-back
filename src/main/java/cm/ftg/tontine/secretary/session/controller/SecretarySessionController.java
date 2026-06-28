package cm.ftg.tontine.secretary.session.controller;

import cm.ftg.tontine.common.dto.ApiResponse;
import cm.ftg.tontine.president.common.TontineIdResolver;
import cm.ftg.tontine.president.session.dto.SessionDto;
import cm.ftg.tontine.secretary.session.dto.CreateBulkSessionsRequest;
import cm.ftg.tontine.secretary.session.dto.CreateCycleRequest;
import cm.ftg.tontine.secretary.session.dto.CreateSessionRequest;
import cm.ftg.tontine.secretary.session.dto.SecretaryCycleDto;
import cm.ftg.tontine.secretary.session.dto.UpdateSessionRequest;
import cm.ftg.tontine.secretary.session.service.SecretarySessionService;
import cm.ftg.tontine.security.AuthenticatedUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/secretary")
public class SecretarySessionController {

    private final SecretarySessionService service;
    private final TontineIdResolver tontineIdResolver;

    public SecretarySessionController(SecretarySessionService service,
                                      TontineIdResolver tontineIdResolver) {
        this.service = service;
        this.tontineIdResolver = tontineIdResolver;
    }

    @GetMapping("/cycles")
    public ApiResponse<List<SecretaryCycleDto>> listCycles(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.listCycles(t, user.id()));
    }

    @PostMapping("/cycles")
    public ApiResponse<SecretaryCycleDto> createCycle(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId,
            @Valid @RequestBody CreateCycleRequest req) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.createCycle(t, user.id(), req), "Cycle créé");
    }

    @GetMapping("/sessions/{id}")
    public ApiResponse<SessionDto> getSession(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.getSession(id, t, user.id()));
    }

    @GetMapping("/sessions")
    public ApiResponse<List<SessionDto>> listSessions(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId,
            @RequestParam UUID cycleId) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.listSessions(t, cycleId, user.id()));
    }

    @PostMapping("/sessions")
    public ApiResponse<SessionDto> createSession(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId,
            @Valid @RequestBody CreateSessionRequest req) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.createSession(t, user.id(), req), "Séance créée");
    }

    @PostMapping("/sessions/bulk")
    public ApiResponse<List<SessionDto>> bulkCreate(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId,
            @Valid @RequestBody CreateBulkSessionsRequest req) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.createBulkSessions(t, user.id(), req), "Séances planifiées");
    }

    @PostMapping("/cycles/{id}/request-closure")
    public ApiResponse<SecretaryCycleDto> requestClosure(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.requestCycleClosure(id, t, user.id()), "Demande de clôture envoyée au Président");
    }

    @PutMapping("/sessions/{id}")
    public ApiResponse<SessionDto> update(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId,
            @Valid @RequestBody UpdateSessionRequest req) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.updateSession(id, t, user.id(), req), "Séance mise à jour");
    }
}
