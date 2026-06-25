package cm.ftg.tontine.treasurer.loan.service;

import cm.ftg.tontine.audit.service.AuditService;
import cm.ftg.tontine.common.exception.ApiException;
import cm.ftg.tontine.common.exception.ResourceNotFoundException;
import cm.ftg.tontine.member.entity.Member;
import cm.ftg.tontine.treasurer.cashbox.entity.CashBox;
import cm.ftg.tontine.treasurer.cashbox.enums.CashBoxType;
import cm.ftg.tontine.treasurer.cashbox.enums.CashMovementKind;
import cm.ftg.tontine.treasurer.cashbox.repository.CashBoxRepository;
import cm.ftg.tontine.treasurer.cashbox.service.CashBoxService;
import cm.ftg.tontine.treasurer.loan.dto.DisburseLoanRequest;
import cm.ftg.tontine.treasurer.loan.dto.LoanDto;
import cm.ftg.tontine.treasurer.loan.dto.RepayLoanRequest;
import cm.ftg.tontine.treasurer.loan.entity.Loan;
import cm.ftg.tontine.treasurer.loan.enums.LoanStatus;
import cm.ftg.tontine.treasurer.loan.repository.LoanRepository;
import cm.ftg.tontine.treasurer.security.TreasurerAccessChecker;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoanService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal TWELVE = new BigDecimal("12");

    private final LoanRepository loanRepository;
    private final CashBoxRepository cashBoxRepository;
    private final CashBoxService cashBoxService;
    private final TreasurerAccessChecker accessChecker;
    private final AuditService auditService;
    private final GuarantorService guarantorService;
    private final LoanRepaymentScheduleService scheduleService;

    public LoanService(LoanRepository loanRepository,
                       CashBoxRepository cashBoxRepository,
                       CashBoxService cashBoxService,
                       TreasurerAccessChecker accessChecker,
                       AuditService auditService,
                       GuarantorService guarantorService,
                       LoanRepaymentScheduleService scheduleService) {
        this.loanRepository = loanRepository;
        this.cashBoxRepository = cashBoxRepository;
        this.cashBoxService = cashBoxService;
        this.accessChecker = accessChecker;
        this.auditService = auditService;
        this.guarantorService = guarantorService;
        this.scheduleService = scheduleService;
    }

    @Transactional(readOnly = true)
    public List<LoanDto> list(UUID tontineId, UUID userId) {
        accessChecker.requireTreasurer(userId, tontineId);
        return loanRepository.findByTontineIdOrderByRequestedAtDesc(tontineId).stream()
                .map(LoanDto::from)
                .toList();
    }

    @Transactional
    public LoanDto disburse(UUID loanId, UUID tontineId, UUID userId, DisburseLoanRequest req) {
        Member treasurer = accessChecker.requireTreasurer(userId, tontineId);
        Loan loan = loadInTontine(loanId, tontineId);
        if (loan.getStatus() != LoanStatus.APPROVED) {
            throw new ApiException("LOAN_INVALID_STATE",
                    "Seul un pret APPROVED peut etre decaisse", HttpStatus.CONFLICT);
        }
        if (!guarantorService.areAllAccepted(loanId)) {
            throw new ApiException("GUARANTORS_PENDING",
                    "Tous les garants doivent avoir accepte avant le decaissement", HttpStatus.CONFLICT);
        }
        loan.setStatus(LoanStatus.DISBURSED);
        loan.setDisbursedAt(Instant.now());
        Loan saved = loanRepository.save(loan);

        debitPrincipal(tontineId, saved.getPrincipal(), CashMovementKind.LOAN_DISBURSEMENT_OUT,
                "Decaissement pret #" + saved.getId() + " membre " + saved.getMemberId(),
                saved.getId().toString(),
                fullName(treasurer));

        scheduleService.generateSchedule(saved);

        auditService.record(userId, "LOAN_DISBURSE", "Loan", saved.getId().toString(),
                tontineId, "{\"amount\":\"***\"}");
        return LoanDto.from(saved);
    }

    @Transactional
    public LoanDto repay(UUID loanId, UUID tontineId, UUID userId, RepayLoanRequest req) {
        Member treasurer = accessChecker.requireTreasurer(userId, tontineId);
        Loan loan = loadInTontine(loanId, tontineId);
        if (loan.getStatus() != LoanStatus.DISBURSED && loan.getStatus() != LoanStatus.REPAYING) {
            throw new ApiException("LOAN_INVALID_STATE",
                    "Le pret ne peut etre rembourse dans son etat actuel", HttpStatus.CONFLICT);
        }
        loan.setTotalRepaid(loan.getTotalRepaid().add(req.amount()));
        if (loan.getTotalRepaid().compareTo(loan.getTotalDue()) >= 0) {
            loan.setStatus(LoanStatus.REPAID);
        } else {
            loan.setStatus(LoanStatus.REPAYING);
        }
        Loan saved = loanRepository.save(loan);

        creditPrincipal(tontineId, req.amount(), CashMovementKind.LOAN_REPAYMENT_IN,
                "Remboursement pret #" + saved.getId() + " membre " + saved.getMemberId(),
                saved.getId().toString(),
                fullName(treasurer));

        scheduleService.applyPayment(saved.getId(), req.amount());

        auditService.record(userId, "LOAN_REPAY", "Loan", saved.getId().toString(),
                tontineId, "{\"amount\":\"***\"}");
        return LoanDto.from(saved);
    }

    @Transactional
    public Loan createPending(UUID tontineId, UUID memberId, BigDecimal principal, BigDecimal annualRatePct,
                              int durationMonths, String purpose, List<UUID> guarantorMemberIds) {
        Loan loan = new Loan();
        loan.setTontineId(tontineId);
        loan.setMemberId(memberId);
        loan.setPrincipal(principal == null ? BigDecimal.ZERO : principal);
        loan.setInterestRate(annualRatePct == null ? BigDecimal.ZERO : annualRatePct);
        loan.setDurationMonths(durationMonths);
        loan.setPurpose(purpose);
        BigDecimal totalDue = computeTotalDue(loan.getPrincipal(), loan.getInterestRate(), durationMonths);
        loan.setTotalDue(totalDue);
        if (durationMonths > 0) {
            loan.setMonthlyPayment(totalDue.divide(BigDecimal.valueOf(durationMonths), 2, RoundingMode.HALF_UP));
        } else {
            loan.setMonthlyPayment(totalDue);
        }
        loan.setStatus(LoanStatus.PENDING);
        Loan saved = loanRepository.save(loan);
        guarantorService.addGuarantors(saved.getId(), guarantorMemberIds);
        return saved;
    }

    private BigDecimal computeTotalDue(BigDecimal principal, BigDecimal annualRatePct, int durationMonths) {
        if (principal == null || principal.signum() <= 0 || durationMonths <= 0) {
            return principal == null ? BigDecimal.ZERO : principal;
        }
        BigDecimal annualRate = annualRatePct == null ? BigDecimal.ZERO :
                annualRatePct.divide(HUNDRED, 8, RoundingMode.HALF_UP);
        BigDecimal interest = principal.multiply(annualRate)
                .multiply(BigDecimal.valueOf(durationMonths))
                .divide(TWELVE, 2, RoundingMode.HALF_UP);
        return principal.add(interest).setScale(2, RoundingMode.HALF_UP);
    }

    private Loan loadInTontine(UUID id, UUID tontineId) {
        Loan loan = loanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", id));
        if (!loan.getTontineId().equals(tontineId)) {
            throw new ApiException("FORBIDDEN", "Pret hors de la tontine active", HttpStatus.FORBIDDEN);
        }
        return loan;
    }

    private void creditPrincipal(UUID tontineId, BigDecimal amount, CashMovementKind kind,
                                 String description, String reference, String recorderFullName) {
        CashBox principal = cashBoxRepository.findByTontineIdAndType(tontineId, CashBoxType.MAIN)
                .orElseThrow(() -> new ApiException("CASHBOX_NOT_CONFIGURED",
                        "Caisse principale introuvable pour la tontine", HttpStatus.valueOf(422)));
        cashBoxService.credit(principal.getId(), amount, kind, reference, description,
                recorderFullName, tontineId);
    }

    private void debitPrincipal(UUID tontineId, BigDecimal amount, CashMovementKind kind,
                                String description, String reference, String recorderFullName) {
        CashBox principal = cashBoxRepository.findByTontineIdAndType(tontineId, CashBoxType.MAIN)
                .orElseThrow(() -> new ApiException("CASHBOX_NOT_CONFIGURED",
                        "Caisse principale introuvable pour la tontine", HttpStatus.valueOf(422)));
        cashBoxService.debit(principal.getId(), amount, kind, reference, description,
                recorderFullName, tontineId);
    }

    private String fullName(Member m) {
        if (m == null) return "";
        String f = m.getFirstName() == null ? "" : m.getFirstName();
        String l = m.getLastName() == null ? "" : m.getLastName();
        return (f + " " + l).trim();
    }
}
