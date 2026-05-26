package cm.ftg.tontine.censor.sanction.controller;

import cm.ftg.tontine.censor.sanction.dto.UnpaidSanctionDto;
import cm.ftg.tontine.censor.sanction.service.CensorUnpaidSanctionService;
import cm.ftg.tontine.common.dto.ApiResponse;
import cm.ftg.tontine.president.common.TontineIdResolver;
import cm.ftg.tontine.security.AuthenticatedUser;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/censor/unpaid-sanctions")
public class CensorUnpaidSanctionController {

    private final CensorUnpaidSanctionService service;
    private final TontineIdResolver tontineIdResolver;

    public CensorUnpaidSanctionController(CensorUnpaidSanctionService service,
                                          TontineIdResolver tontineIdResolver) {
        this.service = service;
        this.tontineIdResolver = tontineIdResolver;
    }

    @GetMapping
    public ApiResponse<List<UnpaidSanctionDto>> list(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.listUnpaid(t, user.id()));
    }
}
