package cm.ftg.tontine.treasurer.loan.service;

import cm.ftg.tontine.audit.service.AuditService;
import cm.ftg.tontine.common.exception.ApiException;
import cm.ftg.tontine.common.exception.ResourceNotFoundException;
import cm.ftg.tontine.treasurer.loan.dto.LoanRepaymentScheduleDto;
import cm.ftg.tontine.treasurer.loan.entity.Loan;
import cm.ftg.tontine.treasurer.loan.entity.LoanRepaymentSchedule;
import cm.ftg.tontine.treasurer.loan.enums.LoanRepaymentScheduleStatus;
import cm.ftg.tontine.treasurer.loan.repository.LoanRepaymentScheduleRepository;
import cm.ftg.tontine.treasurer.loan.repository.LoanRepository;
import cm.ftg.tontine.treasurer.security.TreasurerAccessChecker;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoanRepaymentScheduleService {

    private final LoanRepaymentScheduleRepository repository;
    private final LoanRepository loanRepository;
    private final TreasurerAccessChecker accessChecker;
    private final AuditService auditService;

    public LoanRepaymentScheduleService(LoanRepaymentScheduleRepository repository,
                                        LoanRepository loanRepository,
                                        TreasurerAccessChecker accessChecker,
                                        AuditService auditService) {
        this.repository = repository;
        this.loanRepository = loanRepository;
        this.accessChecker = accessChecker;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<LoanRepaymentScheduleDto> listByLoan(UUID loanId, UUID tontineId, UUID userId) {
        accessChecker.requireTreasurer(userId, tontineId);
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", loanId));
        if (!loan.getTontineId().equals(tontineId)) {
            throw new ApiException("FORBIDDEN", "Pret hors de la tontine active", HttpStatus.FORBIDDEN);
        }
        return repository.findByLoanIdOrderByInstallmentNumber(loanId).stream()
                .map(LoanRepaymentScheduleDto::from)
                .toList();
    }

    /**
     * Generates the flat-interest amortization schedule for a loan immediately after disbursement.
     * Idempotent — skips generation if a schedule already exists for this loan.
     */
    @Transactional
    public void generateSchedule(Loan loan) {
        if (repository.existsByLoanId(loan.getId())) {
            return;
        }

        int n = loan.getDurationMonths();
        if (n <= 0) return;

        BigDecimal principal = loan.getPrincipal();
        BigDecimal totalDue = loan.getTotalDue();
        BigDecimal totalInterest = totalDue.subtract(principal);
        BigDecimal monthlyPayment = loan.getMonthlyPayment();

        BigDecimal monthlyPrincipal = principal.divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP);
        BigDecimal monthlyInterest = totalInterest.divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP);

        Instant base = loan.getDisbursedAt() != null ? loan.getDisbursedAt() : Instant.now();
        ZonedDateTime startDate = ZonedDateTime.ofInstant(base, ZoneOffset.UTC);

        List<LoanRepaymentSchedule> installments = new ArrayList<>(n);
        BigDecimal accPrincipal = BigDecimal.ZERO;
        BigDecimal accInterest = BigDecimal.ZERO;

        for (int i = 1; i <= n; i++) {
            BigDecimal instPrincipal;
            BigDecimal instInterest;
            BigDecimal instTotal;

            if (i == n) {
                // last installment absorbs rounding differences
                instPrincipal = principal.subtract(accPrincipal);
                instInterest = totalInterest.subtract(accInterest);
                instTotal = totalDue.subtract(monthlyPayment.multiply(BigDecimal.valueOf(n - 1)));
            } else {
                instPrincipal = monthlyPrincipal;
                instInterest = monthlyInterest;
                instTotal = monthlyPayment;
                accPrincipal = accPrincipal.add(monthlyPrincipal);
                accInterest = accInterest.add(monthlyInterest);
            }

            Instant dueDate = startDate.plusMonths(i).toInstant();

            LoanRepaymentSchedule s = new LoanRepaymentSchedule();
            s.setLoanId(loan.getId());
            s.setTontineId(loan.getTontineId());
            s.setInstallmentNumber(i);
            s.setDueDate(dueDate);
            s.setPrincipalComponent(instPrincipal.max(BigDecimal.ZERO));
            s.setInterestComponent(instInterest.max(BigDecimal.ZERO));
            s.setTotalAmount(instTotal.max(BigDecimal.ZERO));
            s.setPaidAmount(BigDecimal.ZERO);
            s.setStatus(LoanRepaymentScheduleStatus.PENDING);
            installments.add(s);
        }

        repository.saveAll(installments);

        try {
            auditService.record(loan.getMemberId(), "SCHEDULE_GENERATED", "Loan",
                    loan.getId().toString(), loan.getTontineId(),
                    "{\"installments\":" + n + "}");
        } catch (RuntimeException ignored) {
        }
    }

    /**
     * Applies a repayment amount to pending installments in order (earliest first).
     * Called by LoanService.repay() within the same transaction.
     */
    @Transactional
    public void applyPayment(UUID loanId, BigDecimal amount) {
        List<LoanRepaymentSchedule> pending = repository.findByLoanIdAndStatusInOrderByInstallmentNumber(
                loanId,
                List.of(LoanRepaymentScheduleStatus.PENDING, LoanRepaymentScheduleStatus.PARTIALLY_PAID));

        BigDecimal remaining = amount;
        for (LoanRepaymentSchedule installment : pending) {
            if (remaining.signum() <= 0) break;
            BigDecimal due = installment.getTotalAmount().subtract(installment.getPaidAmount());
            if (remaining.compareTo(due) >= 0) {
                installment.setPaidAmount(installment.getTotalAmount());
                installment.setStatus(LoanRepaymentScheduleStatus.PAID);
                remaining = remaining.subtract(due);
            } else {
                installment.setPaidAmount(installment.getPaidAmount().add(remaining));
                installment.setStatus(LoanRepaymentScheduleStatus.PARTIALLY_PAID);
                remaining = BigDecimal.ZERO;
            }
            repository.save(installment);
        }
    }
}
