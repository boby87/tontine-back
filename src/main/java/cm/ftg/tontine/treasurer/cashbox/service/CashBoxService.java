package cm.ftg.tontine.treasurer.cashbox.service;

import cm.ftg.tontine.common.exception.ApiException;
import cm.ftg.tontine.common.exception.ResourceNotFoundException;
import cm.ftg.tontine.realtime.RealtimeEventPublisher;
import cm.ftg.tontine.treasurer.cashbox.dto.CashBoxDto;
import cm.ftg.tontine.treasurer.cashbox.dto.CashMovementDto;
import cm.ftg.tontine.treasurer.cashbox.entity.CashBox;
import cm.ftg.tontine.treasurer.cashbox.entity.CashMovement;
import cm.ftg.tontine.treasurer.cashbox.enums.CashMovementKind;
import cm.ftg.tontine.treasurer.cashbox.enums.MovementDirection;
import cm.ftg.tontine.treasurer.cashbox.repository.CashBoxRepository;
import cm.ftg.tontine.treasurer.cashbox.repository.CashMovementRepository;
import cm.ftg.tontine.treasurer.security.TreasurerAccessChecker;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CashBoxService {

    private static final int MOVEMENTS_LIMIT = 100;

    private final CashBoxRepository cashBoxRepository;
    private final CashMovementRepository movementRepository;
    private final TreasurerAccessChecker accessChecker;
    private final RealtimeEventPublisher realtime;

    public CashBoxService(CashBoxRepository cashBoxRepository,
                          CashMovementRepository movementRepository,
                          TreasurerAccessChecker accessChecker,
                          RealtimeEventPublisher realtime) {
        this.cashBoxRepository = cashBoxRepository;
        this.movementRepository = movementRepository;
        this.accessChecker = accessChecker;
        this.realtime = realtime;
    }

    @Transactional(readOnly = true)
    public List<CashBoxDto> listForTontine(UUID tontineId, UUID userId) {
        accessChecker.requireTreasurer(userId, tontineId);
        return cashBoxRepository.findByTontineIdOrderByTypeAscNameAsc(tontineId).stream()
                .map(CashBoxDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CashMovementDto> listMovements(UUID cashBoxId, UUID tontineId, UUID userId) {
        accessChecker.requireTreasurer(userId, tontineId);
        CashBox box = cashBoxRepository.findById(cashBoxId)
                .orElseThrow(() -> new ResourceNotFoundException("CashBox", cashBoxId));
        if (!box.getTontineId().equals(tontineId)) {
            throw new ApiException("FORBIDDEN", "Caisse hors de la tontine active", HttpStatus.FORBIDDEN);
        }
        return movementRepository.findByCashBoxIdOrderByRecordedAtDesc(cashBoxId, PageRequest.of(0, MOVEMENTS_LIMIT))
                .stream()
                .map(CashMovementDto::from)
                .toList();
    }

    @Transactional
    public CashMovement debit(UUID cashBoxId, BigDecimal amount, CashMovementKind kind,
                              String reference, String description, String recorderFullName, UUID tontineId) {
        ensurePositive(amount);
        CashBox box = loadInTontine(cashBoxId, tontineId);
        if (box.getBalance().compareTo(amount) < 0) {
            throw new ApiException("INSUFFICIENT_BALANCE",
                    "Solde insuffisant sur la caisse", HttpStatus.valueOf(422));
        }
        box.setBalance(box.getBalance().subtract(amount));
        cashBoxRepository.save(box);
        return persistMovement(box, amount, kind, MovementDirection.OUT, reference, description, recorderFullName);
    }

    @Transactional
    public CashMovement credit(UUID cashBoxId, BigDecimal amount, CashMovementKind kind,
                               String reference, String description, String recorderFullName, UUID tontineId) {
        ensurePositive(amount);
        CashBox box = loadInTontine(cashBoxId, tontineId);
        box.setBalance(box.getBalance().add(amount));
        cashBoxRepository.save(box);
        return persistMovement(box, amount, kind, MovementDirection.IN, reference, description, recorderFullName);
    }

    private CashMovement persistMovement(CashBox box, BigDecimal amount, CashMovementKind kind,
                                         MovementDirection direction, String reference, String description,
                                         String recorderFullName) {
        CashMovement m = new CashMovement();
        m.setTontineId(box.getTontineId());
        m.setCashBoxId(box.getId());
        m.setCashBoxName(box.getName());
        m.setKind(kind);
        m.setAmount(amount);
        m.setDirection(direction);
        m.setReference(reference);
        m.setDescription(description == null ? "" : description);
        m.setBalanceAfter(box.getBalance());
        m.setRecordedByFullName(recorderFullName == null ? "" : recorderFullName);
        CashMovement saved = movementRepository.save(m);
        realtime.toTreasurerDashboard(box.getTontineId(), "cashbox.updated",
                java.util.Map.of(
                        "cashBoxId", box.getId(),
                        "cashBoxName", box.getName(),
                        "kind", kind.name(),
                        "direction", direction.name(),
                        "balanceAfter", box.getBalance()));
        return saved;
    }

    private CashBox loadInTontine(UUID cashBoxId, UUID tontineId) {
        CashBox box = cashBoxRepository.findById(cashBoxId)
                .orElseThrow(() -> new ResourceNotFoundException("CashBox", cashBoxId));
        if (!box.getTontineId().equals(tontineId)) {
            throw new ApiException("FORBIDDEN", "Caisse hors de la tontine active", HttpStatus.FORBIDDEN);
        }
        if (box.isLocked()) {
            throw new ApiException("CASHBOX_LOCKED",
                    "Caisse verrouillee", HttpStatus.CONFLICT);
        }
        return box;
    }

    private void ensurePositive(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new ApiException("INVALID_AMOUNT",
                    "Montant invalide", HttpStatus.valueOf(422));
        }
    }
}
