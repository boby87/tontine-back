package cm.ftg.tontine.censor.sanction.service;

import cm.ftg.tontine.audit.service.AuditService;
import cm.ftg.tontine.censor.sanction.dto.ApplyBatchSanctionsRequest;
import cm.ftg.tontine.censor.sanction.dto.ApplySanctionRequest;
import cm.ftg.tontine.censor.sanction.dto.CancelSanctionRequest;
import cm.ftg.tontine.censor.sanction.dto.ConfirmBatchRequest;
import cm.ftg.tontine.censor.sanction.dto.ConfirmBatchResultDto;
import cm.ftg.tontine.censor.sanction.dto.SanctionLineRequest;
import cm.ftg.tontine.censor.security.CensorAccessChecker;
import cm.ftg.tontine.common.exception.ApiException;
import cm.ftg.tontine.common.exception.ResourceNotFoundException;
import cm.ftg.tontine.member.entity.Member;
import cm.ftg.tontine.member.repository.MemberRepository;
import cm.ftg.tontine.president.sanction.dto.SanctionDto;
import cm.ftg.tontine.president.sanction.entity.Sanction;
import cm.ftg.tontine.president.sanction.enums.SanctionCancelByRole;
import cm.ftg.tontine.president.sanction.enums.SanctionStatus;
import cm.ftg.tontine.president.sanction.enums.SanctionType;
import cm.ftg.tontine.president.sanction.repository.SanctionRepository;
import cm.ftg.tontine.tontine.entity.Tontine;
import cm.ftg.tontine.tontine.entity.TontineRulesEmbeddable;
import cm.ftg.tontine.tontine.repository.TontineRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CensorSanctionService {

    private final SanctionRepository sanctionRepository;
    private final MemberRepository memberRepository;
    private final TontineRepository tontineRepository;
    private final CensorAccessChecker accessChecker;
    private final AuditService auditService;

    public CensorSanctionService(SanctionRepository sanctionRepository,
                                 MemberRepository memberRepository,
                                 TontineRepository tontineRepository,
                                 CensorAccessChecker accessChecker,
                                 AuditService auditService) {
        this.sanctionRepository = sanctionRepository;
        this.memberRepository = memberRepository;
        this.tontineRepository = tontineRepository;
        this.accessChecker = accessChecker;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<SanctionDto> list(UUID tontineId, UUID userId, SanctionStatus status, UUID sessionId) {
        accessChecker.requireCensor(userId, tontineId);
        List<Sanction> entries;
        if (status != null && sessionId != null) {
            entries = sanctionRepository.findByTontineIdAndStatusAndSessionIdOrderByIssuedAtDesc(tontineId, status, sessionId);
        } else if (status != null) {
            entries = sanctionRepository.findByTontineIdAndStatusOrderByIssuedAtDesc(tontineId, status);
        } else if (sessionId != null) {
            entries = sanctionRepository.findByTontineIdAndSessionIdOrderByIssuedAtDesc(tontineId, sessionId);
        } else {
            entries = sanctionRepository.findByTontineIdOrderByIssuedAtDesc(tontineId);
        }
        return entries.stream().map(SanctionDto::from).toList();
    }

    @Transactional
    public SanctionDto apply(UUID tontineId, UUID userId, ApplySanctionRequest req) {
        Member censorMember = accessChecker.requireCensor(userId, tontineId);
        Tontine tontine = tontineRepository.findById(tontineId)
                .orElseThrow(() -> new ResourceNotFoundException("Tontine", tontineId));
        Sanction saved = createAndSave(tontine, censorMember, userId, req.memberId(), req.type(), req.amount(),
                req.reason(), req.severity(), req.customLabel(), req.isFinancial(), req.sessionId());
        auditService.record(userId, "CENSOR_SANCTION_APPLY", "Sanction", saved.getId().toString(), tontineId,
                "{\"type\":\"" + req.type() + "\",\"memberId\":\"" + req.memberId() + "\"}");
        return SanctionDto.from(saved);
    }

    @Transactional
    public List<SanctionDto> applyBatch(UUID tontineId, UUID userId, ApplyBatchSanctionsRequest req) {
        Member censorMember = accessChecker.requireCensor(userId, tontineId);
        Tontine tontine = tontineRepository.findById(tontineId)
                .orElseThrow(() -> new ResourceNotFoundException("Tontine", tontineId));
        List<SanctionDto> result = new ArrayList<>(req.sanctions().size());
        for (SanctionLineRequest line : req.sanctions()) {
            Sanction saved = createAndSave(tontine, censorMember, userId, req.memberId(), line.type(), line.amount(),
                    line.reason(), line.severity(), line.customLabel(), line.isFinancial(), line.sessionId());
            result.add(SanctionDto.from(saved));
        }
        auditService.record(userId, "CENSOR_SANCTION_BATCH_APPLY", "Sanction", req.memberId().toString(), tontineId,
                "{\"memberId\":\"" + req.memberId() + "\",\"count\":" + req.sanctions().size() + "}");
        return result;
    }

    @Transactional(readOnly = true)
    public List<SanctionDto> listAutoDetected(UUID tontineId, UUID userId) {
        accessChecker.requireCensor(userId, tontineId);
        return sanctionRepository
                .findByTontineIdAndAutoDetectedTrueAndStatusOrderByIssuedAtDesc(tontineId, SanctionStatus.PENDING)
                .stream().map(SanctionDto::from).toList();
    }

    @Transactional
    public ConfirmBatchResultDto confirmBatch(UUID tontineId, UUID userId, ConfirmBatchRequest req) {
        accessChecker.requireCensor(userId, tontineId);
        int confirmed = 0;
        int skipped = 0;
        for (UUID id : req.sanctionIds()) {
            Sanction s = sanctionRepository.findById(id).orElse(null);
            if (s == null || !s.getTontineId().equals(tontineId)) {
                skipped++;
                continue;
            }
            if (s.getStatus() != SanctionStatus.PENDING) {
                skipped++;
                continue;
            }
            s.setStatus(SanctionStatus.CONFIRMED);
            s.setResolvedByUserId(userId);
            sanctionRepository.save(s);
            confirmed++;
        }
        auditService.record(userId, "CENSOR_SANCTION_CONFIRM_BATCH", "Sanction", null, tontineId,
                "{\"confirmed\":" + confirmed + ",\"skipped\":" + skipped + ",\"total\":" + req.sanctionIds().size() + "}");
        return new ConfirmBatchResultDto(confirmed, skipped, req.sanctionIds().size());
    }

    @Transactional
    public SanctionDto cancel(UUID id, UUID tontineId, UUID userId, CancelSanctionRequest req) {
        Member censorMember = accessChecker.requireCensor(userId, tontineId);
        Sanction s = sanctionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sanction", id));
        if (!s.getTontineId().equals(tontineId)) {
            throw new ApiException("FORBIDDEN", "Tontine non concordante", HttpStatus.FORBIDDEN);
        }
        if (s.getStatus() == SanctionStatus.PAID) {
            throw new ApiException("SANCTION_PAID_REFUND_REQUIRED",
                    "Sanction deja payee : un remboursement par le tresorier est requis",
                    HttpStatus.CONFLICT);
        }
        if (s.getStatus() == SanctionStatus.CANCELLED || s.getStatus() == SanctionStatus.WAIVED) {
            throw new ApiException("SANCTION_FINAL_STATE",
                    "Cette sanction est dans un etat final et ne peut plus etre annulee",
                    HttpStatus.CONFLICT);
        }
        s.setStatus(SanctionStatus.CANCELLED);
        s.setCancelledAt(Instant.now());
        s.setCancelledByUserId(userId);
        s.setCancelledByFullName(fullName(censorMember));
        s.setCancelReason(req.reason());
        s.setCancelledByRole(SanctionCancelByRole.CENSOR);
        Sanction saved = sanctionRepository.save(s);
        auditService.record(userId, "CENSOR_SANCTION_CANCEL", "Sanction", id.toString(), tontineId,
                "{\"reason\":\"" + escape(req.reason()) + "\"}");
        return SanctionDto.from(saved);
    }

    private Sanction createAndSave(Tontine tontine, Member censorMember, UUID userId, UUID memberId,
                                   SanctionType type, BigDecimal amount, String reason,
                                   cm.ftg.tontine.president.sanction.enums.SanctionSeverity severity,
                                   String customLabel, Boolean isFinancialRequested, UUID sessionId) {
        Member target = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member", memberId));
        if (!target.getTontineId().equals(tontine.getId())) {
            throw new ApiException("FORBIDDEN", "Membre n'appartient pas a cette tontine", HttpStatus.FORBIDDEN);
        }
        boolean financial = isFinancialRequested == null || isFinancialRequested;
        BigDecimal effectiveAmount = amount;
        if (effectiveAmount == null) {
            effectiveAmount = deriveAmountFromRules(tontine, type);
            if (effectiveAmount == null) {
                if (financial) {
                    throw new ApiException("SANCTION_AMOUNT_REQUIRED",
                            "Montant requis pour ce type de sanction financiere",
                            HttpStatus.valueOf(422));
                }
                effectiveAmount = BigDecimal.ZERO;
            }
        }
        Sanction s = new Sanction();
        s.setTontineId(tontine.getId());
        s.setMemberId(memberId);
        s.setMemberFullName(fullName(target));
        s.setSessionId(sessionId);
        s.setType(type);
        s.setCustomLabel(customLabel);
        s.setAmount(effectiveAmount);
        s.setFinancial(financial);
        s.setSeverity(severity);
        s.setReason(reason);
        s.setStatus(SanctionStatus.PENDING);
        s.setAutoDetected(false);
        s.setIssuedByUserId(userId);
        s.setIssuedByFullName(fullName(censorMember));
        return sanctionRepository.save(s);
    }

    private BigDecimal deriveAmountFromRules(Tontine tontine, SanctionType type) {
        TontineRulesEmbeddable rules = tontine.getRules();
        if (rules == null) {
            return null;
        }
        return switch (type) {
            case ABSENCE -> rules.getAbsencePenaltyAmount();
            case LATENESS -> rules.getLatePenaltyAmount();
            case CONTRIBUTION_LATE -> rules.getContributionLatePenaltyAmount();
            case DISCIPLINE, LOAN_DEFAULT, OTHER -> null;
        };
    }

    private String fullName(Member m) {
        return (m.getFirstName() == null ? "" : m.getFirstName()) + " "
                + (m.getLastName() == null ? "" : m.getLastName());
    }

    private String escape(String s) {
        return s == null ? "" : s.replace("\"", "\\\"");
    }
}
