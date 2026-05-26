package cm.ftg.tontine.president.vote.dto;

import cm.ftg.tontine.president.vote.enums.VoteAudience;
import cm.ftg.tontine.president.vote.enums.VoteScope;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record CreateVoteRequest(
        @NotBlank @Size(max = 500) String question,
        @Size(max = 2000) String description,
        @NotEmpty @Size(min = 2, max = 20) List<@NotBlank @Size(max = 300) String> options,
        boolean isAnonymous,
        boolean hideResultsUntilClose,
        @NotNull VoteScope scope,
        @NotNull VoteAudience audience,
        @NotNull Instant opensAt,
        @NotNull Instant closesAt,
        @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal quorumPercent
) {
}
