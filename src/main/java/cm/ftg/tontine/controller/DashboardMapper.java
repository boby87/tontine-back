package cm.ftg.tontine.controller;

import cm.ftg.tontine.service.DashboardSummary;
import org.springframework.stereotype.Component;

/**
 * Convertit un {@link DashboardSummary} (couche service) en {@link DashboardResponse} (couche controller).
 * Aucune entité JPA n'est exposée dans la réponse REST.
 */
@Component
public class DashboardMapper {

    public DashboardResponse toResponse(DashboardSummary summary) {
        var recentTransactions = summary.recentTransactions().stream()
            .map(rt -> new DashboardResponse.RecentTransactionResponse(
                rt.cotisationId(),
                rt.membreId(),
                rt.membreNom(),
                rt.montant(),
                rt.statut(),
                rt.dateOperation()
            ))
            .toList();

        return new DashboardResponse(
            summary.tontineId(),
            summary.tontineNom(),
            summary.totalCagnotte(),
            summary.nextSessionDate(),
            summary.activeMembersCount(),
            summary.pendingSanctionsCount(),
            summary.cycleSessionsCount(),
            summary.completedSessionsCount(),
            summary.cycleProgressPercent(),
            summary.currency(),
            recentTransactions
        );
    }
}

