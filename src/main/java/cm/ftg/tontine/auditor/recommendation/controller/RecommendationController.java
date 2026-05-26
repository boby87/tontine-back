package cm.ftg.tontine.auditor.recommendation.controller;

import cm.ftg.tontine.auditor.recommendation.dto.CreateRecommendationRequest;
import cm.ftg.tontine.auditor.recommendation.dto.RecommendationDto;
import cm.ftg.tontine.auditor.recommendation.dto.UpdateRecommendationStatusRequest;
import cm.ftg.tontine.auditor.recommendation.service.RecommendationService;
import cm.ftg.tontine.common.dto.ApiResponse;
import cm.ftg.tontine.president.common.TontineIdResolver;
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
@RequestMapping("/auditor/recommendations")
public class RecommendationController {

    private final RecommendationService service;
    private final TontineIdResolver tontineIdResolver;

    public RecommendationController(RecommendationService service,
                                    TontineIdResolver tontineIdResolver) {
        this.service = service;
        this.tontineIdResolver = tontineIdResolver;
    }

    @GetMapping
    public ApiResponse<List<RecommendationDto>> list(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.list(t, user.id()));
    }

    @PostMapping
    public ApiResponse<RecommendationDto> create(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId,
            @Valid @RequestBody CreateRecommendationRequest req) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.create(t, user.id(), req), "Recommandation creee");
    }

    @PostMapping("/{id}/status")
    public ApiResponse<RecommendationDto> updateStatus(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRecommendationStatusRequest req) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.updateStatus(id, t, user.id(), req), "Statut mis a jour");
    }
}
