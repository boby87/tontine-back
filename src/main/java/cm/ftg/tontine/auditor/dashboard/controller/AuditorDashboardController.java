package cm.ftg.tontine.auditor.dashboard.controller;

import cm.ftg.tontine.auditor.dashboard.dto.AuditorDashboardDto;
import cm.ftg.tontine.auditor.dashboard.service.AuditorDashboardService;
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
@RequestMapping("/auditor/dashboard")
public class AuditorDashboardController {

    private final AuditorDashboardService service;
    private final TontineIdResolver tontineIdResolver;

    public AuditorDashboardController(AuditorDashboardService service, TontineIdResolver tontineIdResolver) {
        this.service = service;
        this.tontineIdResolver = tontineIdResolver;
    }

    @GetMapping
    public ApiResponse<AuditorDashboardDto> get(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.getDashboard(t, user.id()));
    }
}
