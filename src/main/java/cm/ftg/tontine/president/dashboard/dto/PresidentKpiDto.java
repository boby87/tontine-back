package cm.ftg.tontine.president.dashboard.dto;

import java.math.BigDecimal;

public record PresidentKpiDto(
        BigDecimal totalCashBalance,
        long activeMembersCount,
        long activeLoansCount,
        BigDecimal activeLoansAmount,
        long pendingSanctionsCount,
        BigDecimal pendingSanctionsAmount,
        Long nextSessionInDays,
        Integer nextSessionNumber,
        BigDecimal contributionRate,
        long pendingValidationsCount,
        BigDecimal cycleProgressPercent,
        int cycleCompletedSessions,
        int cycleTotalSessions
) {
}
