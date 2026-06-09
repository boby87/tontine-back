package cm.ftg.tontine.president.presidencytransfer.controller;

import cm.ftg.tontine.common.dto.ApiResponse;
import cm.ftg.tontine.president.common.TontineIdResolver;
import cm.ftg.tontine.president.presidencytransfer.dto.CancelPresidencyTransferRequest;
import cm.ftg.tontine.president.presidencytransfer.dto.InitiatePresidencyTransferRequest;
import cm.ftg.tontine.president.presidencytransfer.dto.PresidencyTransferDto;
import cm.ftg.tontine.president.presidencytransfer.service.PresidencyTransferService;
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
@RequestMapping("/president")
public class PresidentPresidencyTransferController {

    private final PresidencyTransferService service;
    private final TontineIdResolver tontineIdResolver;

    public PresidentPresidencyTransferController(PresidencyTransferService service,
                                                 TontineIdResolver tontineIdResolver) {
        this.service = service;
        this.tontineIdResolver = tontineIdResolver;
    }

    @PostMapping("/presidency-transfer")
    public ApiResponse<PresidencyTransferDto> initiate(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId,
            @Valid @RequestBody InitiatePresidencyTransferRequest req) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.initiate(t, user.id(), req), "Transfert initie");
    }

    @GetMapping("/presidency-transfers")
    public ApiResponse<List<PresidencyTransferDto>> list(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.list(t, user.id()));
    }

    @PostMapping("/presidency-transfers/{id}/cancel")
    public ApiResponse<PresidencyTransferDto> cancel(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId,
            @Valid @RequestBody(required = false) CancelPresidencyTransferRequest req) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.cancel(id, t, user.id(), req), "Transfert annule");
    }
}
