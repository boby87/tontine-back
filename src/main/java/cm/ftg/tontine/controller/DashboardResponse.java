package cm.ftg.tontine.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Réponse REST du dashboard de synthèse d'une tontine.
 * Record immuable — ne jamais exposer d'entités JPA directement.
 */
public record DashboardResponse(
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
    List<RecentTransactionResponse> recentTransactions
) {

    /**
     * Transaction récente visible dans la réponse REST du dashboard.
     */
    public record RecentTransactionResponse(
        Long cotisationId,
        Long membreId,
        String membreNom,
        BigDecimal montant,
        String statut,
        LocalDateTime dateOperation
    ) {}
}

