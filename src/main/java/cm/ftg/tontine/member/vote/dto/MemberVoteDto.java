package cm.ftg.tontine.member.vote.dto;

import cm.ftg.tontine.president.vote.dto.VoteOptionDto;
import cm.ftg.tontine.president.vote.enums.VoteAudience;
import cm.ftg.tontine.president.vote.enums.VoteScope;
import cm.ftg.tontine.president.vote.enums.VoteStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MemberVoteDto(
        UUID id,
        UUID tontineId,
        String question,
        String description,
        List<VoteOptionDto> options,
        boolean isAnonymous,
        boolean hideResultsUntilClose,
        VoteScope scope,
        VoteAudience audience,
        VoteStatus status,
        Instant opensAt,
        Instant closesAt,
        Instant createdAt,
        int totalVoters,
        int totalVoted,
        BigDecimal quorumPercent,
        Boolean passed,
        boolean hasVoted
) {
}
