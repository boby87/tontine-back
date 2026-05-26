package cm.ftg.tontine.auditor.opinion.controller;

import cm.ftg.tontine.auditor.opinion.dto.EmitOpinionRequest;
import cm.ftg.tontine.auditor.opinion.service.AuditorOpinionService;
import cm.ftg.tontine.common.dto.ApiResponse;
import cm.ftg.tontine.president.common.TontineIdResolver;
import cm.ftg.tontine.president.validation.dto.ValidationDto;
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
@RequestMapping("/auditor/validations")
public class AuditorOpinionController {

    private final AuditorOpinionService service;
    private final TontineIdResolver tontineIdResolver;

    public AuditorOpinionController(AuditorOpinionService service,
                                    TontineIdResolver tontineIdResolver) {
        this.service = service;
        this.tontineIdResolver = tontineIdResolver;
    }

    @GetMapping
    public ApiResponse<List<ValidationDto>> list(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.listPending(t, user.id()));
    }

    @PostMapping("/{id}/opinion")
    public ApiResponse<ValidationDto> emit(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId,
            @Valid @RequestBody EmitOpinionRequest req) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.emitOpinion(id, t, user.id(), req), "Avis enregistre");
    }
}
