package cm.ftg.tontine.auditor.anomaly.controller;

import cm.ftg.tontine.auditor.anomaly.dto.AnomalyDto;
import cm.ftg.tontine.auditor.anomaly.dto.CloseAnomalyRequest;
import cm.ftg.tontine.auditor.anomaly.dto.CreateAnomalyRequest;
import cm.ftg.tontine.auditor.anomaly.service.AnomalyService;
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
@RequestMapping("/auditor/anomalies")
public class AnomalyController {

    private final AnomalyService service;
    private final TontineIdResolver tontineIdResolver;

    public AnomalyController(AnomalyService service, TontineIdResolver tontineIdResolver) {
        this.service = service;
        this.tontineIdResolver = tontineIdResolver;
    }

    @GetMapping
    public ApiResponse<List<AnomalyDto>> list(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.list(t, user.id()));
    }

    @PostMapping
    public ApiResponse<AnomalyDto> create(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId,
            @Valid @RequestBody CreateAnomalyRequest req) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        String fullName = (user.email() != null) ? user.email() : user.id().toString();
        return ApiResponse.ok(service.create(t, user.id(), fullName, req), "Anomalie signalee");
    }

    @PostMapping("/{id}/close")
    public ApiResponse<AnomalyDto> close(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId,
            @Valid @RequestBody(required = false) CloseAnomalyRequest req) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.close(id, t, user.id(), req), "Anomalie cloturee");
    }

    @PostMapping("/{id}/reopen")
    public ApiResponse<AnomalyDto> reopen(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.reopen(id, t, user.id()), "Anomalie rouverte");
    }
}
