package cm.ftg.tontine.tontine.dto;

import cm.ftg.tontine.common.enums.ContributionFrequency;
import cm.ftg.tontine.common.enums.TontineStatus;
import cm.ftg.tontine.tontine.entity.Tontine;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record TontineDto(
        UUID id,
        String name,
        String description,
        TontineStatus status,
        BigDecimal contributionAmount,
        ContributionFrequency frequency,
        LocalDate startDate,
        LocalDate endDate,
        int memberCount,
        int maxMembers,
        UUID currentCycleId,
        BigDecimal totalSaved,
        Instant createdAt,
        Instant updatedAt,
        UUID createdByUserId,
        TontineRulesDto rules,
        List<FounderInviteDto> founders
) {

    public static TontineDto from(Tontine t) {
        List<FounderInviteDto> foundersDto = t.getFounders().stream()
                .map(FounderInviteDto::from)
                .toList();
        return new TontineDto(
                t.getId(),
                t.getName(),
                t.getDescription(),
                t.getStatus(),
                t.getContributionAmount(),
                t.getFrequency(),
                t.getStartDate(),
                t.getEndDate(),
                t.getMemberCount(),
                t.getMaxMembers(),
                t.getCurrentCycleId(),
                t.getTotalSaved(),
                t.getCreatedAt(),
                t.getUpdatedAt(),
                t.getCreatedByUserId(),
                TontineRulesDto.from(t.getRules()),
                foundersDto);
    }
}
