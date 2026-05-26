package cm.ftg.tontine.auditor.financialdata.controller;

import cm.ftg.tontine.auditor.financialdata.dto.FinancialDataSnapshotDto;
import cm.ftg.tontine.auditor.financialdata.service.AuditorFinancialDataService;
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
@RequestMapping("/auditor/financial-data")
public class AuditorFinancialDataController {

    private final AuditorFinancialDataService service;
    private final TontineIdResolver tontineIdResolver;

    public AuditorFinancialDataController(AuditorFinancialDataService service,
                                          TontineIdResolver tontineIdResolver) {
        this.service = service;
        this.tontineIdResolver = tontineIdResolver;
    }

    @GetMapping
    public ApiResponse<FinancialDataSnapshotDto> snapshot(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.snapshot(t, user.id()));
    }
}
