package cm.ftg.tontine.auditor.financialdata.service;

import cm.ftg.tontine.auditor.financialdata.dto.FinancialDataSnapshotDto;
import cm.ftg.tontine.auditor.financialdata.dto.FinancialTotalsDto;
import cm.ftg.tontine.auditor.security.AuditorAccessChecker;
import cm.ftg.tontine.president.sanction.dto.SanctionDto;
import cm.ftg.tontine.president.sanction.repository.SanctionRepository;
import cm.ftg.tontine.treasurer.cashbox.dto.CashBoxDto;
import cm.ftg.tontine.treasurer.cashbox.dto.CashMovementDto;
import cm.ftg.tontine.treasurer.cashbox.repository.CashBoxRepository;
import cm.ftg.tontine.treasurer.cashbox.repository.CashMovementRepository;
import cm.ftg.tontine.treasurer.contribution.dto.ContributionDto;
import cm.ftg.tontine.treasurer.contribution.enums.ContributionStatus;
import cm.ftg.tontine.treasurer.contribution.repository.ContributionRepository;
import cm.ftg.tontine.treasurer.distribution.dto.CagnotteDistributionDto;
import cm.ftg.tontine.treasurer.distribution.repository.CagnotteDistributionRepository;
import cm.ftg.tontine.treasurer.expense.dto.ExpenseDto;
import cm.ftg.tontine.treasurer.expense.enums.ExpenseStatus;
import cm.ftg.tontine.treasurer.expense.repository.ExpenseRepository;
import cm.ftg.tontine.treasurer.loan.dto.LoanDto;
import cm.ftg.tontine.treasurer.loan.repository.LoanRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditorFinancialDataService {

    private final CashBoxRepository cashBoxRepository;
    private final CashMovementRepository cashMovementRepository;
    private final ContributionRepository contributionRepository;
    private final LoanRepository loanRepository;
    private final ExpenseRepository expenseRepository;
    private final CagnotteDistributionRepository distributionRepository;
    private final SanctionRepository sanctionRepository;
    private final AuditorAccessChecker accessChecker;

    public AuditorFinancialDataService(CashBoxRepository cashBoxRepository,
                                       CashMovementRepository cashMovementRepository,
                                       ContributionRepository contributionRepository,
                                       LoanRepository loanRepository,
                                       ExpenseRepository expenseRepository,
                                       CagnotteDistributionRepository distributionRepository,
                                       SanctionRepository sanctionRepository,
                                       AuditorAccessChecker accessChecker) {
        this.cashBoxRepository = cashBoxRepository;
        this.cashMovementRepository = cashMovementRepository;
        this.contributionRepository = contributionRepository;
        this.loanRepository = loanRepository;
        this.expenseRepository = expenseRepository;
        this.distributionRepository = distributionRepository;
        this.sanctionRepository = sanctionRepository;
        this.accessChecker = accessChecker;
    }

    @Transactional(readOnly = true)
    public FinancialDataSnapshotDto snapshot(UUID tontineId, UUID userId) {
        accessChecker.requireAuditor(userId, tontineId);

        List<CashBoxDto> cashBoxes = cashBoxRepository.findByTontineIdOrderByTypeAscNameAsc(tontineId)
                .stream().map(CashBoxDto::from).toList();
        List<CashMovementDto> movements = cashMovementRepository
                .findTop100ByTontineIdOrderByRecordedAtDesc(tontineId)
                .stream().map(CashMovementDto::from).toList();
        List<ContributionDto> contributions = contributionRepository
                .findByTontineIdOrderByPaidAtDesc(tontineId)
                .stream().map(ContributionDto::from).toList();
        List<LoanDto> loans = loanRepository.findByTontineIdOrderByRequestedAtDesc(tontineId)
                .stream().map(LoanDto::from).toList();
        List<ExpenseDto> expenses = expenseRepository.findByTontineIdOrderByCreatedAtDesc(tontineId)
                .stream().map(ExpenseDto::from).toList();
        List<CagnotteDistributionDto> distributions = distributionRepository
                .findByTontineIdOrderByCreatedAtDesc(tontineId)
                .stream().map(CagnotteDistributionDto::from).toList();
        List<SanctionDto> sanctions = sanctionRepository.findByTontineIdOrderByIssuedAtDesc(tontineId)
                .stream().map(SanctionDto::from).toList();

        BigDecimal totalBalance = cashBoxes.stream()
                .map(CashBoxDto::balance)
                .filter(b -> b != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal contributionsTotal = contributionRepository
                .sumPaidAmountByTontineAndStatuses(tontineId,
                        List.of(ContributionStatus.PAID, ContributionStatus.PARTIAL));
        BigDecimal expensesTotal = expenseRepository.sumAmountByTontineAndStatuses(tontineId,
                List.of(ExpenseStatus.PAID, ExpenseStatus.APPROVED));
        BigDecimal distributionsTotal = distributionRepository.sumNetAmountByTontine(tontineId);

        FinancialTotalsDto totals = new FinancialTotalsDto(
                totalBalance,
                contributionsTotal != null ? contributionsTotal : BigDecimal.ZERO,
                expensesTotal != null ? expensesTotal : BigDecimal.ZERO,
                distributionsTotal != null ? distributionsTotal : BigDecimal.ZERO);

        return new FinancialDataSnapshotDto(cashBoxes, movements, contributions, loans,
                expenses, distributions, sanctions, totals);
    }
}
