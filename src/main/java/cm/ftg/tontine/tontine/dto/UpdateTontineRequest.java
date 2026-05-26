package cm.ftg.tontine.tontine.dto;

import cm.ftg.tontine.common.enums.ContributionFrequency;
import cm.ftg.tontine.common.enums.TontineStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateTontineRequest(
        @Size(max = 160) String name,
        @Size(max = 1000) String description,
        TontineStatus status,
        @Positive BigDecimal contributionAmount,
        ContributionFrequency frequency,
        Integer maxMembers,
        LocalDate startDate,
        LocalDate endDate,
        @Valid TontineRulesDto rules
) {
}
