package cm.ftg.tontine.member.vote.controller;

import cm.ftg.tontine.common.dto.ApiResponse;
import cm.ftg.tontine.member.vote.dto.CastVoteRequest;
import cm.ftg.tontine.member.vote.dto.MemberVoteDto;
import cm.ftg.tontine.member.vote.dto.VoteBallotDto;
import cm.ftg.tontine.member.vote.service.MemberVoteService;
import cm.ftg.tontine.president.common.TontineIdResolver;
import cm.ftg.tontine.president.vote.enums.VoteStatus;
import cm.ftg.tontine.security.AuthenticatedUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
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
@RequestMapping("/members/me/votes")
public class MemberVoteController {

    private final MemberVoteService service;
    private final TontineIdResolver tontineIdResolver;

    public MemberVoteController(MemberVoteService service, TontineIdResolver tontineIdResolver) {
        this.service = service;
        this.tontineIdResolver = tontineIdResolver;
    }

    @GetMapping
    public ApiResponse<List<MemberVoteDto>> list(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) VoteStatus status,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.listForMember(t, user.id(), status));
    }

    @GetMapping("/{id}")
    public ApiResponse<MemberVoteDto> getById(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.findForMember(id, t, user.id()));
    }

    @PostMapping("/{id}/cast")
    public ApiResponse<VoteBallotDto> cast(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId,
            @Valid @RequestBody CastVoteRequest req) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.cast(id, t, user.id(), req.optionId()), "Vote enregistre");
    }

    @GetMapping("/{id}/ballot-status")
    public ApiResponse<Map<String, Boolean>> ballotStatus(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(Map.of("hasVoted", service.hasVoted(id, t, user.id())));
    }
}
