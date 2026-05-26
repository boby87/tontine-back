package cm.ftg.tontine.president.vote.dto;

import cm.ftg.tontine.president.vote.entity.Vote;
import cm.ftg.tontine.president.vote.enums.VoteAudience;
import cm.ftg.tontine.president.vote.enums.VoteScope;
import cm.ftg.tontine.president.vote.enums.VoteStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record VoteDto(
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
        UUID createdByUserId,
        String createdByFullName,
        Instant createdAt,
        int totalVoters,
        int totalVoted,
        BigDecimal quorumPercent,
        Boolean passed
) {

    public static VoteDto from(Vote v, List<VoteOptionDto> options) {
        return new VoteDto(
                v.getId(), v.getTontineId(), v.getQuestion(), v.getDescription(), options,
                v.isAnonymous(), v.isHideResultsUntilClose(), v.getScope(), v.getAudience(),
                v.getStatus(), v.getOpensAt(), v.getClosesAt(),
                v.getCreatedByUserId(), v.getCreatedByFullName(), v.getCreatedAt(),
                v.getTotalVoters(), v.getTotalVoted(), v.getQuorumPercent(), v.getPassed());
    }
}
