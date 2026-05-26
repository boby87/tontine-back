package cm.ftg.tontine.treasurer.mobilemoney.dto;

import java.math.BigDecimal;

public record MobileMoneyReconciliationDto(
        BigDecimal totalIn,
        BigDecimal totalOut,
        long pendingApprovalCount,
        long matchedCount,
        long unmatchedCount
) {
}
