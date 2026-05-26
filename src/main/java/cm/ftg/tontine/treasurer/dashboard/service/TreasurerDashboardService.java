package cm.ftg.tontine.treasurer.dashboard.service;

import cm.ftg.tontine.president.sanction.enums.SanctionStatus;
import cm.ftg.tontine.president.sanction.repository.SanctionRepository;
import cm.ftg.tontine.treasurer.cashbox.dto.CashBoxDto;
import cm.ftg.tontine.treasurer.cashbox.dto.CashMovementDto;
import cm.ftg.tontine.treasurer.cashbox.entity.CashBox;
import cm.ftg.tontine.treasurer.cashbox.enums.CashTransferStatus;
import cm.ftg.tontine.treasurer.cashbox.repository.CashBoxRepository;
import cm.ftg.tontine.treasurer.cashbox.repository.CashBoxTransferRepository;
import cm.ftg.tontine.treasurer.cashbox.repository.CashMovementRepository;
import cm.ftg.tontine.treasurer.dashboard.dto.TreasurerDashboardDto;
import cm.ftg.tontine.treasurer.expense.enums.ExpenseStatus;
import cm.ftg.tontine.treasurer.expense.repository.ExpenseRepository;
import cm.ftg.tontine.treasurer.loan.enums.LoanStatus;
import cm.ftg.tontine.treasurer.loan.repository.LoanRepository;
import cm.ftg.tontine.treasurer.mobilemoney.enums.MobileMoneyStatus;
import cm.ftg.tontine.treasurer.mobilemoney.repository.MobileMoneyTransactionRepository;
import cm.ftg.tontine.treasurer.security.TreasurerAccessChecker;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TreasurerDashboardService {

    private final TreasurerAccessChecker accessChecker;
    private final CashBoxRepository cashBoxRepository;
    private final CashMovementRepository cashMovementRepository;
    private final CashBoxTransferRepository transferRepository;
    private final MobileMoneyTransactionRepository mobileMoneyRepository;
    private final ExpenseRepository expenseRepository;
    private final LoanRepository loanRepository;
    private final SanctionRepository sanctionRepository;

    public TreasurerDashboardService(TreasurerAccessChecker accessChecker,
                                     CashBoxRepository cashBoxRepository,
                                     CashMovementRepository cashMovementRepository,
                                     CashBoxTransferRepository transferRepository,
                                     MobileMoneyTransactionRepository mobileMoneyRepository,
                                     ExpenseRepository expenseRepository,
                                     LoanRepository loanRepository,
                                     SanctionRepository sanctionRepository) {
        this.accessChecker = accessChecker;
        this.cashBoxRepository = cashBoxRepository;
        this.cashMovementRepository = cashMovementRepository;
        this.transferRepository = transferRepository;
        this.mobileMoneyRepository = mobileMoneyRepository;
        this.expenseRepository = expenseRepository;
        this.loanRepository = loanRepository;
        this.sanctionRepository = sanctionRepository;
    }

    @Transactional(readOnly = true)
    public TreasurerDashboardDto getDashboard(UUID tontineId, UUID userId) {
        accessChecker.requireTreasurer(userId, tontineId);

        List<CashBox> cashBoxes = cashBoxRepository.findByTontineIdOrderByTypeAscNameAsc(tontineId);
        BigDecimal totalBalance = cashBoxes.stream()
                .map(CashBox::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long pendingMobileMoney = mobileMoneyRepository.countByTontineIdAndStatus(
                tontineId, MobileMoneyStatus.PENDING_APPROVAL);
        long pendingTransfers = transferRepository.countByTontineIdAndStatusIn(tontineId,
                List.of(CashTransferStatus.PENDING_PRESIDENT, CashTransferStatus.PENDING_AUDITOR));
        long pendingExpenses = expenseRepository.countByTontineIdAndStatus(
                tontineId, ExpenseStatus.PENDING_VALIDATION);
        long sanctionsToCollect = sanctionRepository.countByTontineIdAndStatus(
                tontineId, SanctionStatus.CONFIRMED);
        long upcomingRepayments = loanRepository.countByTontineIdAndStatusIn(tontineId,
                List.of(LoanStatus.DISBURSED, LoanStatus.REPAYING));

        // pendingDistributions : sessions signees mais non distribuees - calcul approximatif
        // necessite une derived query supplementaire pour etre precis (sessions cagnotteSigned + totalDistributed=0).
        long pendingDistributions = 0L;

        List<CashBoxDto> cashBoxDtos = cashBoxes.stream().map(CashBoxDto::from).toList();
        List<CashMovementDto> recentMovements = cashMovementRepository
                .findTop10ByTontineIdOrderByRecordedAtDesc(tontineId).stream()
                .map(CashMovementDto::from)
                .toList();

        return new TreasurerDashboardDto(totalBalance, cashBoxDtos,
                pendingMobileMoney, pendingTransfers, pendingExpenses, pendingDistributions,
                sanctionsToCollect, upcomingRepayments, recentMovements);
    }
}
