package cm.ftg.tontine.treasurer.extracontrib.controller;

import cm.ftg.tontine.common.dto.ApiResponse;
import cm.ftg.tontine.president.common.TontineIdResolver;
import cm.ftg.tontine.president.extracontrib.dto.ExtraordinaryContributionDto;
import cm.ftg.tontine.security.AuthenticatedUser;
import cm.ftg.tontine.treasurer.extracontrib.dto.CollectExtraContributionRequest;
import cm.ftg.tontine.treasurer.extracontrib.service.TreasurerExtraContributionService;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/treasurer/extra-contributions")
public class TreasurerExtraContributionController {

    private final TreasurerExtraContributionService service;
    private final TontineIdResolver tontineIdResolver;

    public TreasurerExtraContributionController(TreasurerExtraContributionService service,
                                                  TontineIdResolver tontineIdResolver) {
        this.service = service;
        this.tontineIdResolver = tontineIdResolver;
    }

    @GetMapping
    public ApiResponse<List<ExtraordinaryContributionDto>> list(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.list(t, user.id()));
    }

    @PostMapping("/{id}/collect")
    public ApiResponse<ExtraordinaryContributionDto> collect(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId,
            @Valid @RequestBody CollectExtraContributionRequest req) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.collect(id, t, user.id(), req), "Versement enregistre");
    }
}
