package cm.ftg.tontine.treasurer.session.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SessionFinancialReportDto(
        UUID sessionId,
        int sessionNumber,
        Instant sessionDate,
        BigDecimal totalContributions,
        BigDecimal totalExtraContributions,
        BigDecimal totalSanctions,
        BigDecimal totalRepayments,
        BigDecimal totalIncome,
        BigDecimal totalDistribution,
        BigDecimal totalExpenses,
        BigDecimal totalDisbursements,
        BigDecimal totalOutflows,
        BigDecimal netResult,
        List<CashBoxBalanceDto> cashBoxBalances,
        boolean signedByTreasurer,
        boolean signedByPresident,
        Instant treasurerSignedAt,
        Instant presidentSignedAt
) {
}
