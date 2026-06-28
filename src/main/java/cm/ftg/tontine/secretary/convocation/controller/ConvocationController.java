package cm.ftg.tontine.secretary.convocation.controller;

import cm.ftg.tontine.common.dto.ApiResponse;
import cm.ftg.tontine.president.common.TontineIdResolver;
import cm.ftg.tontine.secretary.convocation.dto.ConvocationDto;
import cm.ftg.tontine.secretary.convocation.dto.CreateConvocationRequest;
import cm.ftg.tontine.secretary.convocation.service.ConvocationService;
import cm.ftg.tontine.security.AuthenticatedUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/secretary/convocations")
public class ConvocationController {

    private final ConvocationService service;
    private final TontineIdResolver tontineIdResolver;

    public ConvocationController(ConvocationService service, TontineIdResolver tontineIdResolver) {
        this.service = service;
        this.tontineIdResolver = tontineIdResolver;
    }

    @GetMapping
    public ApiResponse<List<ConvocationDto>> list(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId,
            @RequestParam(required = false) UUID sessionId) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.list(t, user.id(), sessionId));
    }

    @PostMapping
    public ApiResponse<ConvocationDto> create(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId,
            @Valid @RequestBody CreateConvocationRequest req) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.create(t, user.id(), req), "Convocation enregistree");
    }
}
