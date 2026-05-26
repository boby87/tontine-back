package cm.ftg.tontine.treasurer.cashbox.service;

import cm.ftg.tontine.audit.service.AuditService;
import cm.ftg.tontine.auth.entity.UserEntity;
import cm.ftg.tontine.auth.repository.UserRepository;
import cm.ftg.tontine.common.exception.ApiException;
import cm.ftg.tontine.common.exception.ResourceNotFoundException;
import cm.ftg.tontine.treasurer.cashbox.dto.CashBoxTransferDto;
import cm.ftg.tontine.treasurer.cashbox.dto.CreateTransferRequest;
import cm.ftg.tontine.treasurer.cashbox.entity.CashBox;
import cm.ftg.tontine.treasurer.cashbox.entity.CashBoxTransfer;
import cm.ftg.tontine.treasurer.cashbox.enums.CashTransferStatus;
import cm.ftg.tontine.treasurer.cashbox.repository.CashBoxRepository;
import cm.ftg.tontine.treasurer.cashbox.repository.CashBoxTransferRepository;
import cm.ftg.tontine.treasurer.security.TreasurerAccessChecker;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CashBoxTransferService {

    private final CashBoxTransferRepository repository;
    private final CashBoxRepository cashBoxRepository;
    private final TreasurerAccessChecker accessChecker;
    private final AuditService auditService;
    private final UserRepository userRepository;

    public CashBoxTransferService(CashBoxTransferRepository repository,
                                  CashBoxRepository cashBoxRepository,
                                  TreasurerAccessChecker accessChecker,
                                  AuditService auditService,
                                  UserRepository userRepository) {
        this.repository = repository;
        this.cashBoxRepository = cashBoxRepository;
        this.accessChecker = accessChecker;
        this.auditService = auditService;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<CashBoxTransferDto> list(UUID tontineId, UUID userId) {
        accessChecker.requireTreasurer(userId, tontineId);
        return repository.findByTontineIdOrderByRequestedAtDesc(tontineId).stream()
                .map(CashBoxTransferDto::from)
                .toList();
    }

    @Transactional
    public CashBoxTransferDto request(UUID tontineId, UUID userId, CreateTransferRequest req) {
        accessChecker.requireTreasurer(userId, tontineId);
        if (req.fromCashBoxId().equals(req.toCashBoxId())) {
            throw new ApiException("TRANSFER_SAME_CASHBOX",
                    "La caisse source et la caisse destination doivent etre differentes",
                    HttpStatus.valueOf(422));
        }
        CashBox from = cashBoxRepository.findById(req.fromCashBoxId())
                .orElseThrow(() -> new ResourceNotFoundException("CashBox", req.fromCashBoxId()));
        if (!from.getTontineId().equals(tontineId)) {
            throw new ApiException("FORBIDDEN", "Caisse source hors de la tontine active",
                    HttpStatus.FORBIDDEN);
        }
        CashBox to = cashBoxRepository.findById(req.toCashBoxId())
                .orElseThrow(() -> new ResourceNotFoundException("CashBox", req.toCashBoxId()));
        if (!to.getTontineId().equals(tontineId)) {
            throw new ApiException("FORBIDDEN", "Caisse destination hors de la tontine active",
                    HttpStatus.FORBIDDEN);
        }
        if (from.getBalance().compareTo(req.amount()) < 0) {
            throw new ApiException("INSUFFICIENT_BALANCE",
                    "Solde insuffisant sur la caisse source", HttpStatus.valueOf(422));
        }
        UserEntity actor = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("USER_NOT_FOUND",
                        "Utilisateur introuvable", HttpStatus.NOT_FOUND));

        CashBoxTransfer t = new CashBoxTransfer();
        t.setTontineId(tontineId);
        t.setFromCashBoxId(from.getId());
        t.setFromCashBoxName(from.getName());
        t.setToCashBoxId(to.getId());
        t.setToCashBoxName(to.getName());
        t.setAmount(req.amount());
        t.setJustification(req.justification());
        t.setStatus(CashTransferStatus.PENDING_PRESIDENT);
        t.setRequestedByFullName(buildFullName(actor));
        CashBoxTransfer saved = repository.save(t);

        auditService.record(userId, "CASHBOX_TRANSFER_REQUEST", "CashBoxTransfer",
                saved.getId().toString(), tontineId,
                "{\"from\":\"" + from.getName() + "\",\"to\":\"" + to.getName() + "\"}");
        return CashBoxTransferDto.from(saved);
    }

    private String buildFullName(UserEntity u) {
        String first = u.getFirstName() == null ? "" : u.getFirstName();
        String last = u.getLastName() == null ? "" : u.getLastName();
        String full = (first + " " + last).trim();
        return full.isEmpty() ? u.getEmail() : full;
    }
}
