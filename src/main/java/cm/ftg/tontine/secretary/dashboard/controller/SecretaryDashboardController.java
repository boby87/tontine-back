package cm.ftg.tontine.secretary.dashboard.controller;

import cm.ftg.tontine.common.dto.ApiResponse;
import cm.ftg.tontine.president.common.TontineIdResolver;
import cm.ftg.tontine.secretary.dashboard.dto.SecretaryDashboardDto;
import cm.ftg.tontine.secretary.dashboard.service.SecretaryDashboardService;
import cm.ftg.tontine.security.AuthenticatedUser;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/secretary/dashboard")
public class SecretaryDashboardController {

    private final SecretaryDashboardService service;
    private final TontineIdResolver tontineIdResolver;

    public SecretaryDashboardController(SecretaryDashboardService service, TontineIdResolver tontineIdResolver) {
        this.service = service;
        this.tontineIdResolver = tontineIdResolver;
    }

    @GetMapping
    public ApiResponse<SecretaryDashboardDto> get(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.getDashboard(t, user.id()));
    }
}
