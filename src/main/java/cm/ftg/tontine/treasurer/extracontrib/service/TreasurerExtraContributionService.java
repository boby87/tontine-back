package cm.ftg.tontine.treasurer.extracontrib.service;

import cm.ftg.tontine.audit.service.AuditService;
import cm.ftg.tontine.common.exception.ApiException;
import cm.ftg.tontine.common.exception.ResourceNotFoundException;
import cm.ftg.tontine.member.entity.Member;
import cm.ftg.tontine.president.extracontrib.dto.ExtraContributionMemberDto;
import cm.ftg.tontine.president.extracontrib.dto.ExtraordinaryContributionDto;
import cm.ftg.tontine.president.extracontrib.entity.ExtraContributionMember;
import cm.ftg.tontine.president.extracontrib.entity.ExtraordinaryContribution;
import cm.ftg.tontine.president.extracontrib.repository.ExtraContributionMemberRepository;
import cm.ftg.tontine.president.extracontrib.repository.ExtraordinaryContributionRepository;
import cm.ftg.tontine.treasurer.cashbox.entity.CashBox;
import cm.ftg.tontine.treasurer.cashbox.enums.CashBoxType;
import cm.ftg.tontine.treasurer.cashbox.enums.CashMovementKind;
import cm.ftg.tontine.treasurer.cashbox.repository.CashBoxRepository;
import cm.ftg.tontine.treasurer.cashbox.service.CashBoxService;
import cm.ftg.tontine.treasurer.extracontrib.dto.CollectExtraContributionRequest;
import cm.ftg.tontine.treasurer.security.TreasurerAccessChecker;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TreasurerExtraContributionService {

    private final ExtraordinaryContributionRepository repository;
    private final ExtraContributionMemberRepository memberRepository;
    private final CashBoxService cashBoxService;
    private final CashBoxRepository cashBoxRepository;
    private final TreasurerAccessChecker accessChecker;
    private final AuditService auditService;

    public TreasurerExtraContributionService(ExtraordinaryContributionRepository repository,
                                              ExtraContributionMemberRepository memberRepository,
                                              CashBoxService cashBoxService,
                                              CashBoxRepository cashBoxRepository,
                                              TreasurerAccessChecker accessChecker,
                                              AuditService auditService) {
        this.repository = repository;
        this.memberRepository = memberRepository;
        this.cashBoxService = cashBoxService;
        this.cashBoxRepository = cashBoxRepository;
        this.accessChecker = accessChecker;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<ExtraordinaryContributionDto> list(UUID tontineId, UUID userId) {
        accessChecker.requireTreasurer(userId, tontineId);
        return repository.findByTontineIdOrderByCreatedAtDesc(tontineId).stream()
                .map(e -> ExtraordinaryContributionDto.from(e, loadMembers(e.getId())))
                .toList();
    }

    @Transactional
    public ExtraordinaryContributionDto collect(UUID id, UUID tontineId, UUID userId,
                                                 CollectExtraContributionRequest req) {
        Member treasurer = accessChecker.requireTreasurer(userId, tontineId);

        ExtraordinaryContribution extra = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ExtraordinaryContribution", id));
        if (!extra.getTontineId().equals(tontineId)) {
            throw new ApiException("FORBIDDEN", "Cotisation hors de la tontine active",
                    HttpStatus.FORBIDDEN);
        }

        ExtraContributionMember entry = memberRepository
                .findByExtraContribIdAndMemberId(id, req.memberId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "ExtraContributionMember", req.memberId()));

        if (entry.getPaid().compareTo(entry.getExpected()) >= 0) {
            throw new ApiException("EXTRA_CONTRIB_ALREADY_PAID",
                    "Membre deja a jour pour cette cotisation", HttpStatus.CONFLICT);
        }

        entry.setPaid(entry.getPaid().add(req.amount()));
        entry.setPaidAt(Instant.now());
        memberRepository.save(entry);

        extra.setTotalCollected(extra.getTotalCollected().add(req.amount()));
        ExtraordinaryContribution saved = repository.save(extra);

        CashBox principal = cashBoxRepository.findByTontineIdAndType(tontineId, CashBoxType.MAIN)
                .orElseThrow(() -> new ApiException("CASHBOX_PRINCIPAL_MISSING",
                        "Caisse principale introuvable", HttpStatus.valueOf(422)));

        String treasurerFullName = treasurer.getFirstName() + " " + treasurer.getLastName();
        String description = "Cotisation extraordinaire de " + entry.getFullName()
                + " (" + req.paymentMethod() + ")";
        cashBoxService.credit(principal.getId(), req.amount(),
                CashMovementKind.EXTRA_CONTRIBUTION_IN, id.toString(), description,
                treasurerFullName, tontineId);

        auditService.record(userId, "EXTRA_CONTRIB_COLLECT", "ExtraContributionMember",
                entry.getId().toString(), tontineId,
                "{\"memberId\":\"" + req.memberId() + "\",\"amount\":\"***\"}");

        return ExtraordinaryContributionDto.from(saved, loadMembers(saved.getId()));
    }

    private List<ExtraContributionMemberDto> loadMembers(UUID extraId) {
        return memberRepository.findByExtraContribIdOrderByFullNameAsc(extraId).stream()
                .map(ExtraContributionMemberDto::from)
                .toList();
    }
}
