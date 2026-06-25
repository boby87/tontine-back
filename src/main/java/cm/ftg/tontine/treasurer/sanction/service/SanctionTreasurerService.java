package cm.ftg.tontine.treasurer.sanction.service;

import cm.ftg.tontine.audit.service.AuditService;
import cm.ftg.tontine.common.exception.ApiException;
import cm.ftg.tontine.common.exception.ResourceNotFoundException;
import cm.ftg.tontine.member.entity.Member;
import cm.ftg.tontine.president.sanction.dto.SanctionDto;
import cm.ftg.tontine.president.sanction.entity.Sanction;
import cm.ftg.tontine.president.sanction.enums.SanctionStatus;
import cm.ftg.tontine.president.sanction.repository.SanctionRepository;
import cm.ftg.tontine.treasurer.cashbox.entity.CashBox;
import cm.ftg.tontine.treasurer.cashbox.enums.CashBoxType;
import cm.ftg.tontine.treasurer.cashbox.enums.CashMovementKind;
import cm.ftg.tontine.treasurer.cashbox.repository.CashBoxRepository;
import cm.ftg.tontine.treasurer.cashbox.service.CashBoxService;
import cm.ftg.tontine.treasurer.sanction.dto.CollectSanctionRequest;
import cm.ftg.tontine.treasurer.security.TreasurerAccessChecker;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SanctionTreasurerService {

    private final SanctionRepository sanctionRepository;
    private final CashBoxRepository cashBoxRepository;
    private final CashBoxService cashBoxService;
    private final TreasurerAccessChecker accessChecker;
    private final AuditService auditService;

    public SanctionTreasurerService(SanctionRepository sanctionRepository,
                                    CashBoxRepository cashBoxRepository,
                                    CashBoxService cashBoxService,
                                    TreasurerAccessChecker accessChecker,
                                    AuditService auditService) {
        this.sanctionRepository = sanctionRepository;
        this.cashBoxRepository = cashBoxRepository;
        this.cashBoxService = cashBoxService;
        this.accessChecker = accessChecker;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<SanctionDto> list(UUID tontineId, UUID userId) {
        accessChecker.requireTreasurer(userId, tontineId);
        return sanctionRepository.findByTontineIdAndStatusInOrderByIssuedAtDesc(tontineId,
                        List.of(SanctionStatus.CONFIRMED, SanctionStatus.WAIVED)).stream()
                .map(SanctionDto::from)
                .toList();
    }

    @Transactional
    public SanctionDto collect(UUID sanctionId, UUID tontineId, UUID userId, CollectSanctionRequest req) {
        Member treasurer = accessChecker.requireTreasurer(userId, tontineId);
        Sanction s = loadInTontine(sanctionId, tontineId);
        if (s.getStatus() != SanctionStatus.CONFIRMED) {
            throw new ApiException("SANCTION_NOT_COLLECTABLE",
                    "Seules les sanctions CONFIRMED peuvent etre encaissees", HttpStatus.CONFLICT);
        }
        if (!s.isFinancial()) {
            throw new ApiException("SANCTION_NOT_FINANCIAL",
                    "Cette sanction n'est pas financiere", HttpStatus.valueOf(422));
        }
        BigDecimal amount = s.getAmount();
        s.setStatus(SanctionStatus.PAID);
        s.setPaidAt(Instant.now());
        Sanction saved = sanctionRepository.save(s);

        creditPrincipal(tontineId, amount, CashMovementKind.SANCTION_IN,
                "Encaissement sanction #" + saved.getId() + " membre " + saved.getMemberId(),
                saved.getId().toString(),
                fullName(treasurer));

        auditService.record(userId, "SANCTION_COLLECT", "Sanction", saved.getId().toString(),
                tontineId, "{\"amount\":\"***\"}");
        return SanctionDto.from(saved);
    }

    @Transactional
    public SanctionDto refund(UUID sanctionId, UUID tontineId, UUID userId) {
        Member treasurer = accessChecker.requireTreasurer(userId, tontineId);
        Sanction s = loadInTontine(sanctionId, tontineId);
        if (s.getStatus() != SanctionStatus.WAIVED && s.getStatus() != SanctionStatus.CANCELLED) {
            throw new ApiException("SANCTION_NOT_REFUNDABLE",
                    "Seules les sanctions WAIVED ou CANCELLED peuvent etre remboursees",
                    HttpStatus.CONFLICT);
        }
        if (!s.isRefundInitiated()) {
            throw new ApiException("SANCTION_REFUND_NOT_INITIATED",
                    "Le remboursement n'a pas ete initie par le president",
                    HttpStatus.CONFLICT);
        }
        BigDecimal amount = s.getAmount();
        s.setRefundInitiated(false);
        Sanction saved = sanctionRepository.save(s);

        debitPrincipal(tontineId, amount, CashMovementKind.SANCTION_REFUND_OUT,
                "Remboursement sanction #" + saved.getId() + " membre " + saved.getMemberId(),
                saved.getId().toString(),
                fullName(treasurer));

        auditService.record(userId, "SANCTION_REFUND", "Sanction", saved.getId().toString(),
                tontineId, "{\"amount\":\"***\"}");
        return SanctionDto.from(saved);
    }

    private Sanction loadInTontine(UUID id, UUID tontineId) {
        Sanction s = sanctionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sanction", id));
        if (!s.getTontineId().equals(tontineId)) {
            throw new ApiException("FORBIDDEN", "Sanction hors de la tontine active", HttpStatus.FORBIDDEN);
        }
        return s;
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
