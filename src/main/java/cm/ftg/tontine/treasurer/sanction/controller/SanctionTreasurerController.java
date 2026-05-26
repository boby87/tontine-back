package cm.ftg.tontine.treasurer.sanction.controller;

import cm.ftg.tontine.common.dto.ApiResponse;
import cm.ftg.tontine.president.common.TontineIdResolver;
import cm.ftg.tontine.president.sanction.dto.SanctionDto;
import cm.ftg.tontine.security.AuthenticatedUser;
import cm.ftg.tontine.treasurer.sanction.dto.CollectSanctionRequest;
import cm.ftg.tontine.treasurer.sanction.service.SanctionTreasurerService;
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
@RequestMapping("/treasurer/sanctions")
public class SanctionTreasurerController {

    private final SanctionTreasurerService service;
    private final TontineIdResolver tontineIdResolver;

    public SanctionTreasurerController(SanctionTreasurerService service,
                                       TontineIdResolver tontineIdResolver) {
        this.service = service;
        this.tontineIdResolver = tontineIdResolver;
    }

    @GetMapping
    public ApiResponse<List<SanctionDto>> list(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.list(t, user.id()));
    }

    @PostMapping("/{id}/collect")
    public ApiResponse<SanctionDto> collect(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId,
            @Valid @RequestBody CollectSanctionRequest req) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.collect(id, t, user.id(), req), "Sanction encaissee");
    }

    @PostMapping("/{id}/refund")
    public ApiResponse<SanctionDto> refund(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.refund(id, t, user.id()), "Sanction remboursee");
    }
}
