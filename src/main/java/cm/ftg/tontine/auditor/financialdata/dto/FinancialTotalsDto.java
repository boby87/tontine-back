package cm.ftg.tontine.auditor.financialdata.dto;

import java.math.BigDecimal;

public record FinancialTotalsDto(
        BigDecimal totalBalance,
        BigDecimal contributions,
        BigDecimal expenses,
        BigDecimal distributions
) {
}
