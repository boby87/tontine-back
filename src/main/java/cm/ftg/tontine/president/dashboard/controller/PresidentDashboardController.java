package cm.ftg.tontine.president.dashboard.controller;

import cm.ftg.tontine.common.dto.ApiResponse;
import cm.ftg.tontine.president.common.TontineIdResolver;
import cm.ftg.tontine.president.dashboard.dto.PresidentDashboardDto;
import cm.ftg.tontine.president.dashboard.service.PresidentDashboardService;
import cm.ftg.tontine.security.AuthenticatedUser;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/president/dashboard")
public class PresidentDashboardController {

    private final PresidentDashboardService service;
    private final TontineIdResolver tontineIdResolver;

    public PresidentDashboardController(PresidentDashboardService service, TontineIdResolver tontineIdResolver) {
        this.service = service;
        this.tontineIdResolver = tontineIdResolver;
    }

    @GetMapping
    public ApiResponse<PresidentDashboardDto> get(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.getDashboard(t, user.id()));
    }
}
