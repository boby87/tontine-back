package cm.ftg.tontine.member.vote.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CastVoteRequest(
        @NotNull UUID optionId
) {
}
