package cm.ftg.tontine.treasurer.contribution.controller;

import cm.ftg.tontine.common.dto.ApiResponse;
import cm.ftg.tontine.president.common.TontineIdResolver;
import cm.ftg.tontine.security.AuthenticatedUser;
import cm.ftg.tontine.treasurer.contribution.dto.AdvancePaymentRequest;
import cm.ftg.tontine.treasurer.contribution.dto.ContributionDto;
import cm.ftg.tontine.treasurer.contribution.dto.PayContributionRequest;
import cm.ftg.tontine.treasurer.contribution.service.ContributionService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/treasurer/contributions")
public class ContributionController {

    private final ContributionService service;
    private final TontineIdResolver tontineIdResolver;

    public ContributionController(ContributionService service, TontineIdResolver tontineIdResolver) {
        this.service = service;
        this.tontineIdResolver = tontineIdResolver;
    }

    @GetMapping
    public ApiResponse<List<ContributionDto>> list(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId,
            @RequestParam UUID sessionId) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.listBySession(t, sessionId, user.id()));
    }

    @PostMapping("/{id}/pay")
    public ApiResponse<ContributionDto> pay(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId,
            @Valid @RequestBody PayContributionRequest req) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.pay(id, t, user.id(), req), "Cotisation encaissee");
    }

    @PostMapping("/advance")
    public ApiResponse<List<ContributionDto>> advance(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId,
            @Valid @RequestBody AdvancePaymentRequest req) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.advance(t, user.id(), req), "Cotisation anticipee enregistree");
    }
}
