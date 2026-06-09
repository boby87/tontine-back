package cm.ftg.tontine.member.vote.dto;

import java.time.Instant;
import java.util.UUID;

public record VoteBallotDto(
        UUID voteId,
        UUID optionId,
        String optionLabel,
        Instant castAt
) {
}
