package cm.ftg.tontine.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Résumé du dashboard d'une tontine.
 * Contient les indicateurs clés du cycle en cours.
 */
public record DashboardSummary(
    Long tontineId,
    String tontineNom,
    BigDecimal totalCagnotte,
    LocalDate nextSessionDate,
    int activeMembersCount,
    int pendingSanctionsCount,
    int cycleSessionsCount,
    int completedSessionsCount,
    BigDecimal cycleProgressPercent,
    String currency,
    List<RecentTransaction> recentTransactions
) {}

