package cm.ftg.tontine.president.cycle.controller;

import cm.ftg.tontine.common.dto.ApiResponse;
import cm.ftg.tontine.president.common.TontineIdResolver;
import cm.ftg.tontine.president.cycle.service.PresidentCycleService;
import cm.ftg.tontine.secretary.session.dto.SecretaryCycleDto;
import cm.ftg.tontine.security.AuthenticatedUser;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/president")
public class PresidentCycleController {

    private final PresidentCycleService service;
    private final TontineIdResolver tontineIdResolver;

    public PresidentCycleController(PresidentCycleService service,
                                    TontineIdResolver tontineIdResolver) {
        this.service = service;
        this.tontineIdResolver = tontineIdResolver;
    }

    @GetMapping("/cycles/pending-closure")
    public ApiResponse<List<SecretaryCycleDto>> pendingClosures(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.listPendingClosures(t, user.id()));
    }

    @PostMapping("/cycles/{id}/close")
    public ApiResponse<SecretaryCycleDto> closeCycle(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.closeCycle(id, t, user.id()), "Cycle clôturé avec succès");
    }
}
