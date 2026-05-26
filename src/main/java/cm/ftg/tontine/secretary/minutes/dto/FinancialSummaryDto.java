package cm.ftg.tontine.secretary.minutes.dto;

import java.math.BigDecimal;

public record FinancialSummaryDto(
        BigDecimal totalCollected,
        BigDecimal totalDistributed,
        String beneficiaryFullName
) {
}
