package cm.ftg.tontine.president.extracontrib.controller;

import cm.ftg.tontine.common.dto.ApiResponse;
import cm.ftg.tontine.president.common.TontineIdResolver;
import cm.ftg.tontine.president.extracontrib.dto.CreateExtraordinaryContributionRequest;
import cm.ftg.tontine.president.extracontrib.dto.ExtraordinaryContributionDto;
import cm.ftg.tontine.president.extracontrib.service.ExtraordinaryContributionService;
import cm.ftg.tontine.security.AuthenticatedUser;
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
@RequestMapping("/president/extraordinary-contributions")
public class ExtraordinaryContributionController {

    private final ExtraordinaryContributionService service;
    private final TontineIdResolver tontineIdResolver;

    public ExtraordinaryContributionController(ExtraordinaryContributionService service,
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

    @PostMapping
    public ApiResponse<ExtraordinaryContributionDto> create(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId,
            @Valid @RequestBody CreateExtraordinaryContributionRequest req) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.create(t, user.id(), req), "Cotisation extraordinaire creee");
    }

    @PostMapping("/{id}/close")
    public ApiResponse<ExtraordinaryContributionDto> close(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.close(id, t, user.id()), "Collecte cloturee");
    }

    @PostMapping("/{id}/distribute")
    public ApiResponse<ExtraordinaryContributionDto> distribute(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.distribute(id, t, user.id()), "Cotisation distribuee");
    }
}
