package cm.ftg.tontine.censor.contestation.controller;

import cm.ftg.tontine.censor.contestation.dto.DecideContestationRequest;
import cm.ftg.tontine.censor.contestation.service.ContestationCensorService;
import cm.ftg.tontine.common.dto.ApiResponse;
import cm.ftg.tontine.president.common.TontineIdResolver;
import cm.ftg.tontine.president.sanction.dto.SanctionDto;
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
@RequestMapping("/censor/contestations")
public class ContestationCensorController {

    private final ContestationCensorService service;
    private final TontineIdResolver tontineIdResolver;

    public ContestationCensorController(ContestationCensorService service,
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

    @PostMapping("/{id}/decide")
    public ApiResponse<SanctionDto> decide(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId,
            @Valid @RequestBody DecideContestationRequest req) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        String fullName = (user.email() != null) ? user.email() : user.id().toString();
        return ApiResponse.ok(service.decide(id, t, user.id(), fullName, req),
                "Decision de contestation enregistree");
    }
}
