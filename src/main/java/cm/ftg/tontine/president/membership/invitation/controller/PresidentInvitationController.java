package cm.ftg.tontine.president.membership.invitation.controller;

import cm.ftg.tontine.common.dto.ApiResponse;
import cm.ftg.tontine.president.common.TontineIdResolver;
import cm.ftg.tontine.president.membership.invitation.dto.CancelInvitationRequest;
import cm.ftg.tontine.president.membership.invitation.dto.InviteMemberRequest;
import cm.ftg.tontine.president.membership.invitation.dto.MembershipInvitationDto;
import cm.ftg.tontine.president.membership.invitation.service.InvitationService;
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
@RequestMapping("/president/membership")
public class PresidentInvitationController {

    private final InvitationService service;
    private final TontineIdResolver tontineIdResolver;

    public PresidentInvitationController(InvitationService service, TontineIdResolver tontineIdResolver) {
        this.service = service;
        this.tontineIdResolver = tontineIdResolver;
    }

    @PostMapping("/invite")
    public ApiResponse<MembershipInvitationDto> invite(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId,
            @Valid @RequestBody InviteMemberRequest req) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.invite(t, user.id(), req), "Invitation envoyee");
    }

    @GetMapping("/invitations")
    public ApiResponse<List<MembershipInvitationDto>> list(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.list(t, user.id()));
    }

    @PostMapping("/invitations/{id}/resend")
    public ApiResponse<MembershipInvitationDto> resend(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.resend(id, t, user.id()), "Invitation relancee");
    }

    @PostMapping("/invitations/{id}/cancel")
    public ApiResponse<MembershipInvitationDto> cancel(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId,
            @Valid @RequestBody(required = false) CancelInvitationRequest req) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.cancel(id, t, user.id(), req), "Invitation annulee");
    }
}
