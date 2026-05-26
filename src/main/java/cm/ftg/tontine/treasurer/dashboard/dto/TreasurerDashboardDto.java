package cm.ftg.tontine.treasurer.dashboard.dto;

import cm.ftg.tontine.treasurer.cashbox.dto.CashBoxDto;
import cm.ftg.tontine.treasurer.cashbox.dto.CashMovementDto;
import java.math.BigDecimal;
import java.util.List;

public record TreasurerDashboardDto(
        BigDecimal totalBalance,
        List<CashBoxDto> cashBoxes,
        long pendingMobileMoney,
        long pendingTransfers,
        long pendingExpenses,
        long pendingDistributions,
        long sanctionsToCollect,
        long upcomingRepayments,
        List<CashMovementDto> recentMovements
) {
}
