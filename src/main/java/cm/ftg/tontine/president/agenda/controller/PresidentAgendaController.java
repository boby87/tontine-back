package cm.ftg.tontine.president.agenda.controller;

import cm.ftg.tontine.common.dto.ApiResponse;
import cm.ftg.tontine.president.common.TontineIdResolver;
import cm.ftg.tontine.secretary.agenda.dto.AgendaDraftDto;
import cm.ftg.tontine.secretary.agenda.dto.RequestChangesRequest;
import cm.ftg.tontine.secretary.agenda.service.AgendaDraftService;
import cm.ftg.tontine.security.AuthenticatedUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/president/agendas")
public class PresidentAgendaController {

    private final AgendaDraftService service;
    private final TontineIdResolver tontineIdResolver;

    public PresidentAgendaController(AgendaDraftService service,
                                     TontineIdResolver tontineIdResolver) {
        this.service = service;
        this.tontineIdResolver = tontineIdResolver;
    }

    @GetMapping
    public ApiResponse<List<AgendaDraftDto>> listPending(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId,
            @RequestParam(required = false) UUID sessionId) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.listPending(t, user.id(), sessionId));
    }

    @PostMapping("/{id}/approve")
    public ApiResponse<AgendaDraftDto> approve(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.approve(id, t, user.id()), "Ordre du jour approuve");
    }

    @PostMapping("/{id}/request-changes")
    public ApiResponse<AgendaDraftDto> requestChanges(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId,
            @Valid @RequestBody RequestChangesRequest req) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.requestChanges(id, t, user.id(), req.comment()),
                "Modifications demandees");
    }
}
