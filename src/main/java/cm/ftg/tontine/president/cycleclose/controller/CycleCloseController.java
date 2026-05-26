package cm.ftg.tontine.president.cycleclose.controller;

import cm.ftg.tontine.common.dto.ApiResponse;
import cm.ftg.tontine.president.common.TontineIdResolver;
import cm.ftg.tontine.president.cycleclose.dto.CycleCloseDto;
import cm.ftg.tontine.president.cycleclose.dto.SignCycleCloseRequest;
import cm.ftg.tontine.president.cycleclose.service.CycleCloseService;
import cm.ftg.tontine.security.AuthenticatedUser;
import jakarta.validation.Valid;
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
@RequestMapping("/president/cycle-close")
public class CycleCloseController {

    private final CycleCloseService service;
    private final TontineIdResolver tontineIdResolver;

    public CycleCloseController(CycleCloseService service, TontineIdResolver tontineIdResolver) {
        this.service = service;
        this.tontineIdResolver = tontineIdResolver;
    }

    @GetMapping
    public ApiResponse<CycleCloseDto> getCurrent(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.getOrCreateCurrent(t, user.id()));
    }

    @PostMapping("/check/{key}")
    public ApiResponse<CycleCloseDto> check(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable String key,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.check(t, user.id(), key), "Element de checklist marque comme fait");
    }

    @PostMapping("/sign")
    public ApiResponse<CycleCloseDto> sign(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId,
            @Valid @RequestBody SignCycleCloseRequest req) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.sign(t, user.id(), req), "Cloture signee");
    }
}
