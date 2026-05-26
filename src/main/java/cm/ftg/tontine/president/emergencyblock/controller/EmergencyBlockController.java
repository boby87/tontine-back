package cm.ftg.tontine.president.emergencyblock.controller;

import cm.ftg.tontine.common.dto.ApiResponse;
import cm.ftg.tontine.president.common.TontineIdResolver;
import cm.ftg.tontine.president.emergencyblock.dto.CreateEmergencyBlockRequest;
import cm.ftg.tontine.president.emergencyblock.dto.EmergencyBlockDto;
import cm.ftg.tontine.president.emergencyblock.dto.LiftEmergencyBlockRequest;
import cm.ftg.tontine.president.emergencyblock.service.EmergencyBlockService;
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
@RequestMapping("/president/emergency-blocks")
public class EmergencyBlockController {

    private final EmergencyBlockService service;
    private final TontineIdResolver tontineIdResolver;

    public EmergencyBlockController(EmergencyBlockService service, TontineIdResolver tontineIdResolver) {
        this.service = service;
        this.tontineIdResolver = tontineIdResolver;
    }

    @GetMapping
    public ApiResponse<List<EmergencyBlockDto>> list(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.list(t, user.id()));
    }

    @PostMapping
    public ApiResponse<EmergencyBlockDto> activate(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId,
            @Valid @RequestBody CreateEmergencyBlockRequest req) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.activate(t, user.id(), req), "Blocage active");
    }

    @PostMapping("/{id}/lift")
    public ApiResponse<EmergencyBlockDto> lift(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId,
            @Valid @RequestBody LiftEmergencyBlockRequest req) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.lift(id, t, user.id(), req), "Blocage leve");
    }
}
