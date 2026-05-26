package cm.ftg.tontine.treasurer.mobilemoney.service;

import cm.ftg.tontine.audit.service.AuditService;
import cm.ftg.tontine.common.exception.ApiException;
import cm.ftg.tontine.common.exception.ResourceNotFoundException;
import cm.ftg.tontine.member.entity.Member;
import cm.ftg.tontine.realtime.RealtimeEventPublisher;
import cm.ftg.tontine.treasurer.cashbox.entity.CashBox;
import cm.ftg.tontine.treasurer.cashbox.enums.CashBoxType;
import cm.ftg.tontine.treasurer.cashbox.enums.CashMovementKind;
import cm.ftg.tontine.treasurer.cashbox.enums.MovementDirection;
import cm.ftg.tontine.treasurer.cashbox.repository.CashBoxRepository;
import cm.ftg.tontine.treasurer.cashbox.service.CashBoxService;
import cm.ftg.tontine.treasurer.mobilemoney.dto.ApproveMobileMoneyRequest;
import cm.ftg.tontine.treasurer.mobilemoney.dto.MobileMoneyReconciliationDto;
import cm.ftg.tontine.treasurer.mobilemoney.dto.MobileMoneySendRequest;
import cm.ftg.tontine.treasurer.mobilemoney.dto.MobileMoneyTransactionDto;
import cm.ftg.tontine.treasurer.mobilemoney.dto.RejectMobileMoneyRequest;
import cm.ftg.tontine.treasurer.mobilemoney.entity.MobileMoneyTransaction;
import cm.ftg.tontine.treasurer.mobilemoney.enums.MobileMoneyStatus;
import cm.ftg.tontine.treasurer.mobilemoney.repository.MobileMoneyTransactionRepository;
import cm.ftg.tontine.treasurer.security.TreasurerAccessChecker;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MobileMoneyService {

    private final MobileMoneyTransactionRepository repository;
    private final CashBoxService cashBoxService;
    private final CashBoxRepository cashBoxRepository;
    private final TreasurerAccessChecker accessChecker;
    private final AuditService auditService;
    private final RealtimeEventPublisher realtime;

    public MobileMoneyService(MobileMoneyTransactionRepository repository,
                              CashBoxService cashBoxService,
                              CashBoxRepository cashBoxRepository,
                              TreasurerAccessChecker accessChecker,
                              AuditService auditService,
                              RealtimeEventPublisher realtime) {
        this.repository = repository;
        this.cashBoxService = cashBoxService;
        this.cashBoxRepository = cashBoxRepository;
        this.accessChecker = accessChecker;
        this.auditService = auditService;
        this.realtime = realtime;
    }

    @Transactional(readOnly = true)
    public List<MobileMoneyTransactionDto> list(UUID tontineId, UUID userId) {
        accessChecker.requireTreasurer(userId, tontineId);
        return repository.findByTontineIdOrderByReceivedAtDesc(tontineId).stream()
                .map(MobileMoneyTransactionDto::from)
                .toList();
    }

    @Transactional
    public MobileMoneyTransactionDto approve(UUID id, UUID tontineId, UUID userId,
                                              ApproveMobileMoneyRequest req) {
        Member treasurer = accessChecker.requireTreasurer(userId, tontineId);
        MobileMoneyTransaction tx = loadInTontine(id, tontineId);
        if (tx.getStatus() != MobileMoneyStatus.PENDING_APPROVAL) {
            throw new ApiException("MOBILE_MONEY_INVALID_STATE",
                    "Seule une transaction en attente d'approbation peut etre approuvee",
                    HttpStatus.CONFLICT);
        }

        String treasurerFullName = treasurer.getFirstName() + " " + treasurer.getLastName();
        tx.setStatus(MobileMoneyStatus.APPROVED);
        tx.setReviewedAt(Instant.now());
        tx.setReviewedByFullName(treasurerFullName);
        if (req != null && req.contributionId() != null) {
            tx.setContributionId(req.contributionId());
        }
        repository.save(tx);

        CashBox principal = cashBoxRepository.findByTontineIdAndType(tontineId, CashBoxType.PRINCIPAL)
                .orElseThrow(() -> new ApiException("CASHBOX_PRINCIPAL_MISSING",
                        "Caisse principale introuvable", HttpStatus.valueOf(422)));

        String description = "Mobile Money " + tx.getProvider() + " ref=" + tx.getExternalReference();
        cashBoxService.credit(principal.getId(), tx.getAmount(), CashMovementKind.MOBILE_MONEY_IN,
                tx.getExternalReference(), description, treasurerFullName, tontineId);

        tx.setStatus(MobileMoneyStatus.COMPLETED);
        MobileMoneyTransaction saved = repository.save(tx);

        auditService.record(userId, "MOBILE_MONEY_APPROVE", "MobileMoneyTransaction",
                id.toString(), tontineId, null);
        realtime.toTreasurerDashboard(tontineId, "mobile_money.received",
                MobileMoneyTransactionDto.from(saved));
        return MobileMoneyTransactionDto.from(saved);
    }

    @Transactional
    public MobileMoneyTransactionDto reject(UUID id, UUID tontineId, UUID userId,
                                             RejectMobileMoneyRequest req) {
        Member treasurer = accessChecker.requireTreasurer(userId, tontineId);
        MobileMoneyTransaction tx = loadInTontine(id, tontineId);
        if (tx.getStatus() != MobileMoneyStatus.PENDING_APPROVAL) {
            throw new ApiException("MOBILE_MONEY_INVALID_STATE",
                    "Seule une transaction en attente d'approbation peut etre rejetee",
                    HttpStatus.CONFLICT);
        }
        tx.setStatus(MobileMoneyStatus.REJECTED);
        tx.setReviewedAt(Instant.now());
        tx.setReviewedByFullName(treasurer.getFirstName() + " " + treasurer.getLastName());
        tx.setRejectionReason(req.reason());
        MobileMoneyTransaction saved = repository.save(tx);
        auditService.record(userId, "MOBILE_MONEY_REJECT", "MobileMoneyTransaction",
                id.toString(), tontineId, null);
        return MobileMoneyTransactionDto.from(saved);
    }

    @Transactional
    public MobileMoneyTransactionDto send(UUID tontineId, UUID userId, MobileMoneySendRequest req) {
        Member treasurer = accessChecker.requireTreasurer(userId, tontineId);
        if (req.pin() == null || req.pin().isBlank()) {
            throw new ApiException("MOBILE_MONEY_INVALID_PIN",
                    "PIN trésorier requis", HttpStatus.valueOf(422));
        }

        MobileMoneyTransaction tx = new MobileMoneyTransaction();
        tx.setTontineId(tontineId);
        tx.setProvider(req.provider());
        tx.setDirection(MovementDirection.OUT);
        tx.setAmount(req.amount());
        tx.setToPhone(req.toPhone());
        tx.setExternalReference(UUID.randomUUID().toString());
        tx.setStatus(MobileMoneyStatus.APPROVED);
        tx.setReviewedAt(Instant.now());
        tx.setReviewedByFullName(treasurer.getFirstName() + " " + treasurer.getLastName());
        MobileMoneyTransaction saved = repository.save(tx);

        auditService.record(userId, "MOBILE_MONEY_SEND", "MobileMoneyTransaction",
                saved.getId().toString(), tontineId,
                "{\"toPhone\":\"" + req.toPhone() + "\",\"amount\":\"***\"}");
        return MobileMoneyTransactionDto.from(saved);
    }

    @Transactional(readOnly = true)
    public MobileMoneyReconciliationDto reconciliation(UUID tontineId, UUID userId) {
        accessChecker.requireTreasurer(userId, tontineId);
        BigDecimal totalIn = repository.sumAmountByDirection(tontineId, MovementDirection.IN);
        BigDecimal totalOut = repository.sumAmountByDirection(tontineId, MovementDirection.OUT);
        long pending = repository.countByTontineIdAndStatus(tontineId, MobileMoneyStatus.PENDING_APPROVAL);
        long matched = repository.countMatched(tontineId);
        long unmatched = repository.countUnmatched(tontineId);
        return new MobileMoneyReconciliationDto(
                totalIn == null ? BigDecimal.ZERO : totalIn,
                totalOut == null ? BigDecimal.ZERO : totalOut,
                pending, matched, unmatched);
    }

    private MobileMoneyTransaction loadInTontine(UUID id, UUID tontineId) {
        MobileMoneyTransaction tx = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MobileMoneyTransaction", id));
        if (!tx.getTontineId().equals(tontineId)) {
            throw new ApiException("FORBIDDEN", "Tontine non concordante", HttpStatus.FORBIDDEN);
        }
        return tx;
    }
}
