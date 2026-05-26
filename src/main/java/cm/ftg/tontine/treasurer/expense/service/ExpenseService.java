package cm.ftg.tontine.treasurer.expense.service;

import cm.ftg.tontine.audit.service.AuditService;
import cm.ftg.tontine.auth.entity.UserEntity;
import cm.ftg.tontine.auth.repository.UserRepository;
import cm.ftg.tontine.common.exception.ApiException;
import cm.ftg.tontine.common.exception.ResourceNotFoundException;
import cm.ftg.tontine.tontine.entity.Tontine;
import cm.ftg.tontine.tontine.repository.TontineRepository;
import cm.ftg.tontine.treasurer.cashbox.entity.CashBox;
import cm.ftg.tontine.treasurer.cashbox.enums.CashMovementKind;
import cm.ftg.tontine.treasurer.cashbox.repository.CashBoxRepository;
import cm.ftg.tontine.treasurer.cashbox.service.CashBoxService;
import cm.ftg.tontine.treasurer.common.enums.PaymentMethod;
import cm.ftg.tontine.treasurer.expense.dto.CreateExpenseRequest;
import cm.ftg.tontine.treasurer.expense.dto.ExpenseDto;
import cm.ftg.tontine.treasurer.expense.entity.Expense;
import cm.ftg.tontine.treasurer.expense.enums.ExpenseStatus;
import cm.ftg.tontine.treasurer.expense.repository.ExpenseRepository;
import cm.ftg.tontine.treasurer.security.TreasurerAccessChecker;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExpenseService {

    private final ExpenseRepository repository;
    private final TontineRepository tontineRepository;
    private final CashBoxRepository cashBoxRepository;
    private final CashBoxService cashBoxService;
    private final TreasurerAccessChecker accessChecker;
    private final AuditService auditService;
    private final UserRepository userRepository;

    public ExpenseService(ExpenseRepository repository,
                          TontineRepository tontineRepository,
                          CashBoxRepository cashBoxRepository,
                          CashBoxService cashBoxService,
                          TreasurerAccessChecker accessChecker,
                          AuditService auditService,
                          UserRepository userRepository) {
        this.repository = repository;
        this.tontineRepository = tontineRepository;
        this.cashBoxRepository = cashBoxRepository;
        this.cashBoxService = cashBoxService;
        this.accessChecker = accessChecker;
        this.auditService = auditService;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<ExpenseDto> list(UUID tontineId, UUID userId) {
        accessChecker.requireTreasurer(userId, tontineId);
        return repository.findByTontineIdOrderByCreatedAtDesc(tontineId).stream()
                .map(ExpenseDto::from)
                .toList();
    }

    @Transactional
    public ExpenseDto create(UUID tontineId, UUID userId, CreateExpenseRequest req) {
        accessChecker.requireTreasurer(userId, tontineId);
        Tontine tontine = tontineRepository.findById(tontineId)
                .orElseThrow(() -> new ResourceNotFoundException("Tontine", tontineId));
        if (tontine.getRules() == null || tontine.getRules().getExpenseCapWithoutValidation() == null) {
            throw new ApiException("TONTINE_RULES_MISSING",
                    "Regles de la tontine indisponibles", HttpStatus.CONFLICT);
        }
        BigDecimal cap = tontine.getRules().getExpenseCapWithoutValidation();

        CashBox box = cashBoxRepository.findById(req.cashBoxId())
                .orElseThrow(() -> new ResourceNotFoundException("CashBox", req.cashBoxId()));
        if (!box.getTontineId().equals(tontineId)) {
            throw new ApiException("FORBIDDEN", "Caisse hors de la tontine active", HttpStatus.FORBIDDEN);
        }

        UserEntity actor = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("USER_NOT_FOUND",
                        "Utilisateur introuvable", HttpStatus.NOT_FOUND));
        String fullName = buildFullName(actor);

        Expense e = new Expense();
        e.setTontineId(tontineId);
        e.setCashBoxId(box.getId());
        e.setCategory(req.category());
        e.setAmount(req.amount());
        e.setDescription(req.description());
        e.setVendor(req.vendor());
        e.setReceiptFileName(req.receiptFileName());
        e.setCap(cap);
        e.setCreatedByFullName(fullName);

        boolean needsValidation = req.amount().compareTo(cap) > 0;
        if (needsValidation) {
            e.setNeedsValidation(true);
            e.setStatus(ExpenseStatus.PENDING_VALIDATION);
        } else {
            e.setNeedsValidation(false);
            e.setStatus(ExpenseStatus.APPROVED);
            e.setPaymentMethod(PaymentMethod.CASH);
        }

        Expense saved = repository.save(e);

        if (!needsValidation) {
            cashBoxService.debit(box.getId(), saved.getAmount(), CashMovementKind.EXPENSE_OUT,
                    "EXPENSE:" + saved.getId(),
                    "Depense " + saved.getCategory().name() + " - " + saved.getDescription(),
                    fullName, tontineId);
            saved.setStatus(ExpenseStatus.PAID);
            saved.setPaidAt(Instant.now());
            saved = repository.save(saved);
        }

        auditService.record(userId, "EXPENSE_CREATE", "Expense", saved.getId().toString(), tontineId,
                "{\"category\":\"" + saved.getCategory().name() + "\",\"needsValidation\":"
                        + saved.isNeedsValidation() + "}");
        return ExpenseDto.from(saved);
    }

    private String buildFullName(UserEntity u) {
        String first = u.getFirstName() == null ? "" : u.getFirstName();
        String last = u.getLastName() == null ? "" : u.getLastName();
        String full = (first + " " + last).trim();
        return full.isEmpty() ? u.getEmail() : full;
    }
}
