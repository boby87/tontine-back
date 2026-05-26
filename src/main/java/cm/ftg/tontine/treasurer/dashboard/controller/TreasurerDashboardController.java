package cm.ftg.tontine.treasurer.dashboard.controller;

import cm.ftg.tontine.common.dto.ApiResponse;
import cm.ftg.tontine.president.common.TontineIdResolver;
import cm.ftg.tontine.security.AuthenticatedUser;
import cm.ftg.tontine.treasurer.dashboard.dto.TreasurerDashboardDto;
import cm.ftg.tontine.treasurer.dashboard.service.TreasurerDashboardService;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/treasurer/dashboard")
public class TreasurerDashboardController {

    private final TreasurerDashboardService service;
    private final TontineIdResolver tontineIdResolver;

    public TreasurerDashboardController(TreasurerDashboardService service,
                                        TontineIdResolver tontineIdResolver) {
        this.service = service;
        this.tontineIdResolver = tontineIdResolver;
    }

    @GetMapping
    public ApiResponse<TreasurerDashboardDto> get(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.getDashboard(t, user.id()));
    }
}
