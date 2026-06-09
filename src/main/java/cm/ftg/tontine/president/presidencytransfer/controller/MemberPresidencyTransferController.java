package cm.ftg.tontine.president.presidencytransfer.controller;

import cm.ftg.tontine.common.dto.ApiResponse;
import cm.ftg.tontine.president.presidencytransfer.dto.DeclinePresidencyTransferRequest;
import cm.ftg.tontine.president.presidencytransfer.dto.PresidencyTransferDto;
import cm.ftg.tontine.president.presidencytransfer.service.PresidencyAcceptService;
import cm.ftg.tontine.president.presidencytransfer.service.PresidencyTransferService;
import cm.ftg.tontine.security.AuthenticatedUser;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/members/me/presidency-transfer")
public class MemberPresidencyTransferController {

    private final PresidencyTransferService service;
    private final PresidencyAcceptService acceptService;

    public MemberPresidencyTransferController(PresidencyTransferService service,
                                              PresidencyAcceptService acceptService) {
        this.service = service;
        this.acceptService = acceptService;
    }

    @GetMapping("/pending")
    public ApiResponse<PresidencyTransferDto> pending(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.ok(service.getPendingForMember(user.id()));
    }

    @PostMapping("/{id}/accept")
    public ApiResponse<PresidencyTransferDto> accept(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id) {
        return ApiResponse.ok(acceptService.accept(id, user.id()),
                "Transfert accepte. Rafraichissez votre session (/auth/refresh) pour appliquer le nouveau role.");
    }

    @PostMapping("/{id}/decline")
    public ApiResponse<PresidencyTransferDto> decline(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id,
            @Valid @RequestBody DeclinePresidencyTransferRequest req) {
        return ApiResponse.ok(service.decline(id, user.id(), req), "Transfert refuse");
    }
}
