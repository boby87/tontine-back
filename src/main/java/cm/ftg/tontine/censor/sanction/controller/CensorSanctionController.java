package cm.ftg.tontine.censor.sanction.controller;

import cm.ftg.tontine.censor.sanction.dto.ApplyBatchSanctionsRequest;
import cm.ftg.tontine.censor.sanction.dto.ApplySanctionRequest;
import cm.ftg.tontine.censor.sanction.dto.CancelSanctionRequest;
import cm.ftg.tontine.censor.sanction.dto.ConfirmBatchRequest;
import cm.ftg.tontine.censor.sanction.dto.ConfirmBatchResultDto;
import cm.ftg.tontine.censor.sanction.service.CensorSanctionService;
import cm.ftg.tontine.common.dto.ApiResponse;
import cm.ftg.tontine.president.common.TontineIdResolver;
import cm.ftg.tontine.president.sanction.dto.SanctionDto;
import cm.ftg.tontine.president.sanction.enums.SanctionStatus;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/censor/sanctions")
public class CensorSanctionController {

    private final CensorSanctionService service;
    private final TontineIdResolver tontineIdResolver;

    public CensorSanctionController(CensorSanctionService service, TontineIdResolver tontineIdResolver) {
        this.service = service;
        this.tontineIdResolver = tontineIdResolver;
    }

    @GetMapping
    public ApiResponse<List<SanctionDto>> list(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId,
            @RequestParam(value = "status", required = false) SanctionStatus status,
            @RequestParam(value = "sessionId", required = false) UUID sessionId) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.list(t, user.id(), status, sessionId));
    }

    @PostMapping
    public ApiResponse<SanctionDto> apply(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId,
            @Valid @RequestBody ApplySanctionRequest req) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.apply(t, user.id(), req), "Sanction appliquee");
    }

    @PostMapping("/batch")
    public ApiResponse<List<SanctionDto>> applyBatch(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId,
            @Valid @RequestBody ApplyBatchSanctionsRequest req) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.applyBatch(t, user.id(), req), "Sanctions appliquees");
    }

    @GetMapping("/auto-detected")
    public ApiResponse<List<SanctionDto>> listAutoDetected(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.listAutoDetected(t, user.id()));
    }

    @PostMapping("/confirm-batch")
    public ApiResponse<ConfirmBatchResultDto> confirmBatch(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId,
            @Valid @RequestBody ConfirmBatchRequest req) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.confirmBatch(t, user.id(), req), "Sanctions confirmees");
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<SanctionDto> cancel(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId,
            @Valid @RequestBody CancelSanctionRequest req) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.cancel(id, t, user.id(), req), "Sanction annulee");
    }
}
