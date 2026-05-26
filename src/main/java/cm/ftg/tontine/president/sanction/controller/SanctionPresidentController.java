package cm.ftg.tontine.president.sanction.controller;

import cm.ftg.tontine.common.dto.ApiResponse;
import cm.ftg.tontine.president.common.TontineIdResolver;
import cm.ftg.tontine.president.sanction.dto.SanctionDto;
import cm.ftg.tontine.president.sanction.dto.WaiveSanctionRequest;
import cm.ftg.tontine.president.sanction.service.SanctionPresidentService;
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
@RequestMapping("/president/sanctions")
public class SanctionPresidentController {

    private final SanctionPresidentService service;
    private final TontineIdResolver tontineIdResolver;

    public SanctionPresidentController(SanctionPresidentService service, TontineIdResolver tontineIdResolver) {
        this.service = service;
        this.tontineIdResolver = tontineIdResolver;
    }

    @GetMapping
    public ApiResponse<List<SanctionDto>> list(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.listForReview(t, user.id()));
    }

    @PostMapping("/{id}/waive")
    public ApiResponse<SanctionDto> waive(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId,
            @Valid @RequestBody WaiveSanctionRequest req) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        String fullName = (user.email() != null) ? user.email() : user.id().toString();
        return ApiResponse.ok(service.waive(id, t, user.id(), fullName, req), "Sanction levee");
    }

    @PostMapping("/{id}/confirm")
    public ApiResponse<SanctionDto> confirm(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.confirm(id, t, user.id()), "Sanction confirmee");
    }
}
