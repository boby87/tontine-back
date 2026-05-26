package cm.ftg.tontine.secretary.membership.controller;

import cm.ftg.tontine.common.dto.ApiResponse;
import cm.ftg.tontine.president.common.TontineIdResolver;
import cm.ftg.tontine.president.membership.dto.MembershipFileDto;
import cm.ftg.tontine.president.membership.enums.MembershipFileKind;
import cm.ftg.tontine.secretary.membership.dto.SecretaryMembershipReviewRequest;
import cm.ftg.tontine.secretary.membership.service.SecretaryMembershipService;
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
@RequestMapping("/secretary/membership")
public class SecretaryMembershipController {

    private final SecretaryMembershipService service;
    private final TontineIdResolver tontineIdResolver;

    public SecretaryMembershipController(SecretaryMembershipService service,
                                         TontineIdResolver tontineIdResolver) {
        this.service = service;
        this.tontineIdResolver = tontineIdResolver;
    }

    @GetMapping
    public ApiResponse<List<MembershipFileDto>> list(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId,
            @RequestParam(required = false) MembershipFileKind kind) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.list(t, user.id(), kind));
    }

    @PostMapping("/{id}/review")
    public ApiResponse<MembershipFileDto> review(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId,
            @Valid @RequestBody SecretaryMembershipReviewRequest req) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.review(id, t, user.id(), req), "Decision enregistree");
    }
}
