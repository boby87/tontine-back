package cm.ftg.tontine.auditor.financialdata.dto;

import cm.ftg.tontine.president.sanction.dto.SanctionDto;
import cm.ftg.tontine.treasurer.cashbox.dto.CashBoxDto;
import cm.ftg.tontine.treasurer.cashbox.dto.CashMovementDto;
import cm.ftg.tontine.treasurer.contribution.dto.ContributionDto;
import cm.ftg.tontine.treasurer.distribution.dto.CagnotteDistributionDto;
import cm.ftg.tontine.treasurer.expense.dto.ExpenseDto;
import cm.ftg.tontine.treasurer.loan.dto.LoanDto;
import java.util.List;

public record FinancialDataSnapshotDto(
        List<CashBoxDto> cashBoxes,
        List<CashMovementDto> movements,
        List<ContributionDto> contributions,
        List<LoanDto> loans,
        List<ExpenseDto> expenses,
        List<CagnotteDistributionDto> distributions,
        List<SanctionDto> sanctions,
        FinancialTotalsDto totals
) {
}
