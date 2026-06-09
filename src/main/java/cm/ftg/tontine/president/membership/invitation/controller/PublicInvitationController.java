package cm.ftg.tontine.president.membership.invitation.controller;

import cm.ftg.tontine.common.dto.ApiResponse;
import cm.ftg.tontine.president.membership.invitation.dto.AcceptInvitationRequest;
import cm.ftg.tontine.president.membership.invitation.dto.AcceptInvitationResponse;
import cm.ftg.tontine.president.membership.invitation.dto.InvitationPreviewDto;
import cm.ftg.tontine.president.membership.invitation.service.InvitationAcceptService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints publics (sans authentification) consommes par le candidat depuis
 * le lien d'invitation. Whitelistes dans {@code SecurityConfig} (/auth/invitations/**).
 */
@RestController
@RequestMapping("/auth/invitations")
public class PublicInvitationController {

    private final InvitationAcceptService service;

    public PublicInvitationController(InvitationAcceptService service) {
        this.service = service;
    }

    @GetMapping("/{token}/preview")
    public ApiResponse<InvitationPreviewDto> preview(@PathVariable String token) {
        return ApiResponse.ok(service.preview(token));
    }

    @PostMapping("/{token}/accept")
    public ApiResponse<AcceptInvitationResponse> accept(@PathVariable String token,
                                                        @Valid @RequestBody AcceptInvitationRequest req) {
        return ApiResponse.ok(service.accept(token, req), "Invitation acceptee");
    }
}
