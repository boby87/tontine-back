package cm.ftg.tontine.member.controller;

import cm.ftg.tontine.common.dto.ApiResponse;
import cm.ftg.tontine.member.dto.MemberSummaryDto;
import cm.ftg.tontine.member.service.MemberService;
import cm.ftg.tontine.security.AuthenticatedUser;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/members/me")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping("/summary")
    public ApiResponse<MemberSummaryDto> summary(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId) {
        return ApiResponse.ok(memberService.getMySummary(user.id(), tontineId));
    }

    @GetMapping("/contributions")
    public ApiResponse<List<Object>> contributions(@AuthenticationPrincipal AuthenticatedUser user) {
        // Module Cotisations a implementer (entites Session + Contribution).
        return ApiResponse.ok(List.of());
    }

    @GetMapping("/loans")
    public ApiResponse<List<Object>> loans(@AuthenticationPrincipal AuthenticatedUser user) {
        // Module Prets a implementer.
        return ApiResponse.ok(List.of());
    }

    @GetMapping("/planning")
    public ApiResponse<List<Object>> planning(@AuthenticationPrincipal AuthenticatedUser user) {
        // Module Sessions a implementer (planning des prochaines seances).
        return ApiResponse.ok(List.of());
    }
}
