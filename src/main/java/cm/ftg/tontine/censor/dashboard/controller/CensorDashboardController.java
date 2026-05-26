package cm.ftg.tontine.censor.dashboard.controller;

import cm.ftg.tontine.censor.dashboard.dto.CensorDashboardDto;
import cm.ftg.tontine.censor.dashboard.service.CensorDashboardService;
import cm.ftg.tontine.common.dto.ApiResponse;
import cm.ftg.tontine.president.common.TontineIdResolver;
import cm.ftg.tontine.security.AuthenticatedUser;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/censor/dashboard")
public class CensorDashboardController {

    private final CensorDashboardService service;
    private final TontineIdResolver tontineIdResolver;

    public CensorDashboardController(CensorDashboardService service, TontineIdResolver tontineIdResolver) {
        this.service = service;
        this.tontineIdResolver = tontineIdResolver;
    }

    @GetMapping
    public ApiResponse<CensorDashboardDto> get(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.getDashboard(t, user.id()));
    }
}
