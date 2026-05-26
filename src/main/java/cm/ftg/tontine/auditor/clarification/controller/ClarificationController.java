package cm.ftg.tontine.auditor.clarification.controller;

import cm.ftg.tontine.auditor.clarification.dto.ClarificationDto;
import cm.ftg.tontine.auditor.clarification.dto.CreateClarificationRequest;
import cm.ftg.tontine.auditor.clarification.dto.EvaluateClarificationRequest;
import cm.ftg.tontine.auditor.clarification.dto.SimulateClarificationResponseRequest;
import cm.ftg.tontine.auditor.clarification.service.ClarificationService;
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
@RequestMapping("/auditor/clarifications")
public class ClarificationController {

    private final ClarificationService service;
    private final TontineIdResolver tontineIdResolver;

    public ClarificationController(ClarificationService service,
                                   TontineIdResolver tontineIdResolver) {
        this.service = service;
        this.tontineIdResolver = tontineIdResolver;
    }

    @GetMapping
    public ApiResponse<List<ClarificationDto>> list(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.list(t, user.id()));
    }

    @PostMapping
    public ApiResponse<ClarificationDto> create(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId,
            @Valid @RequestBody CreateClarificationRequest req) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.create(t, user.id(), req), "Clarification creee");
    }

    @PostMapping("/{id}/simulate-response")
    public ApiResponse<ClarificationDto> simulateResponse(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId,
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) SimulateClarificationResponseRequest req) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.simulateResponse(id, t, user.id(), req),
                "Reponse simulee enregistree");
    }

    @PostMapping("/{id}/evaluate")
    public ApiResponse<ClarificationDto> evaluate(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId,
            @PathVariable UUID id,
            @Valid @RequestBody EvaluateClarificationRequest req) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.evaluate(id, t, user.id(), req), "Clarification evaluee");
    }
}
