package cm.ftg.tontine.member.controller;

import cm.ftg.tontine.common.dto.ApiResponse;
import cm.ftg.tontine.member.dto.MemberSessionView;
import cm.ftg.tontine.member.dto.MemberSummaryDto;
import cm.ftg.tontine.member.service.MemberService;
import cm.ftg.tontine.member.service.MemberSessionService;
import cm.ftg.tontine.secretary.session.dto.SecretaryCycleDto;
import cm.ftg.tontine.security.AuthenticatedUser;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/members/me")
public class MemberController {

    private final MemberService memberService;
    private final MemberSessionService memberSessionService;

    public MemberController(MemberService memberService,
                            MemberSessionService memberSessionService) {
        this.memberService = memberService;
        this.memberSessionService = memberSessionService;
    }

    @GetMapping("/summary")
    public ApiResponse<MemberSummaryDto> summary(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId) {
        return ApiResponse.ok(memberService.getMySummary(user.id(), tontineId));
    }

    @GetMapping("/contributions")
    public ApiResponse<List<Object>> contributions(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.ok(List.of());
    }

    @GetMapping("/loans")
    public ApiResponse<List<Object>> loans(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.ok(List.of());
    }

    @GetMapping("/planning")
    public ApiResponse<List<Object>> planning(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.ok(List.of());
    }

    @GetMapping("/cycles")
    public ApiResponse<List<SecretaryCycleDto>> cycles(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId) {
        return ApiResponse.ok(memberSessionService.listCycles(user.id(), tontineId));
    }

    @GetMapping("/cycles/{cycleId}/sessions")
    public ApiResponse<List<MemberSessionView>> sessionsByCycle(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID cycleId,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId) {
        return ApiResponse.ok(memberSessionService.listSessionsByCycle(cycleId, user.id(), tontineId));
    }

    @GetMapping("/sessions")
    public ApiResponse<List<MemberSessionView>> sessions(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId) {
        return ApiResponse.ok(memberSessionService.listSessionsWithApprovedAgenda(user.id(), tontineId));
    }

    @GetMapping("/sessions/{sessionId}")
    public ApiResponse<MemberSessionView> session(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID sessionId,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId) {
        return ApiResponse.ok(memberSessionService.getSessionDetail(sessionId, user.id(), tontineId));
    }
}
