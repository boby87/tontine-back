package cm.ftg.tontine.tontine.dto;

import cm.ftg.tontine.common.enums.ContributionFrequency;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CreateTontineRequest(
        @NotBlank @Size(max = 160) String name,
        @Size(max = 1000) String description,
        @NotNull LocalDate startDate,
        @NotNull @Positive BigDecimal contributionAmount,
        @NotNull ContributionFrequency frequency,
        @NotNull @Min(2) Integer maxMembers,
        @NotNull @Valid TontineRulesDto rules,
        @NotEmpty @Valid List<FounderInviteDto> founders
) {
}
