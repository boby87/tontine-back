package cm.ftg.tontine.secretary.agenda.controller;

import cm.ftg.tontine.common.dto.ApiResponse;
import cm.ftg.tontine.president.common.TontineIdResolver;
import cm.ftg.tontine.secretary.agenda.dto.AgendaDraftDto;
import cm.ftg.tontine.secretary.agenda.dto.CreateAgendaDraftRequest;
import cm.ftg.tontine.secretary.agenda.dto.UpdateAgendaItemsRequest;
import cm.ftg.tontine.secretary.agenda.service.AgendaDraftService;
import cm.ftg.tontine.security.AuthenticatedUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/secretary/agendas")
public class AgendaDraftController {

    private final AgendaDraftService service;
    private final TontineIdResolver tontineIdResolver;

    public AgendaDraftController(AgendaDraftService service, TontineIdResolver tontineIdResolver) {
        this.service = service;
        this.tontineIdResolver = tontineIdResolver;
    }

    @GetMapping("/next-session-number")
    public ApiResponse<Integer> nextSessionNumber(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.nextSessionNumber(t, user.id()));
    }

    @GetMapping
    public ApiResponse<List<AgendaDraftDto>> list(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId,
            @RequestParam(required = false) UUID sessionId) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.list(t, user.id(), sessionId));
    }

    @GetMapping("/{id}")
    public ApiResponse<AgendaDraftDto> findById(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.findById(id, t, user.id()));
    }

    @PostMapping
    public ApiResponse<AgendaDraftDto> create(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId,
            @Valid @RequestBody CreateAgendaDraftRequest req) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.create(t, user.id(), req), "Brouillon cree");
    }

    @PutMapping("/{id}/items")
    public ApiResponse<AgendaDraftDto> updateItems(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId,
            @Valid @RequestBody UpdateAgendaItemsRequest req) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.updateItems(id, t, user.id(), req.items()), "Points mis a jour");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        service.delete(id, t, user.id());
        return ApiResponse.ok(null, "ODJ supprime");
    }

    @PostMapping("/{id}/submit")
    public ApiResponse<AgendaDraftDto> submit(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.submit(id, t, user.id()), "Soumis au president");
    }
}
