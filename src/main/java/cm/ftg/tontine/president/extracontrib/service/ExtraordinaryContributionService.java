package cm.ftg.tontine.president.extracontrib.service;

import cm.ftg.tontine.audit.service.AuditService;
import cm.ftg.tontine.common.enums.MemberStatus;
import cm.ftg.tontine.common.exception.ApiException;
import cm.ftg.tontine.common.exception.ResourceNotFoundException;
import cm.ftg.tontine.member.entity.Member;
import cm.ftg.tontine.member.repository.MemberRepository;
import cm.ftg.tontine.president.extracontrib.dto.CreateExtraordinaryContributionRequest;
import cm.ftg.tontine.president.extracontrib.dto.ExtraContributionMemberDto;
import cm.ftg.tontine.president.extracontrib.dto.ExtraordinaryContributionDto;
import cm.ftg.tontine.president.extracontrib.entity.ExtraContributionMember;
import cm.ftg.tontine.president.extracontrib.entity.ExtraordinaryContribution;
import cm.ftg.tontine.president.extracontrib.enums.ExtraContributionStatus;
import cm.ftg.tontine.president.extracontrib.repository.ExtraContributionMemberRepository;
import cm.ftg.tontine.president.extracontrib.repository.ExtraordinaryContributionRepository;
import cm.ftg.tontine.president.security.PresidentAccessChecker;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExtraordinaryContributionService {

    private final ExtraordinaryContributionRepository repository;
    private final ExtraContributionMemberRepository memberRepository;
    private final MemberRepository tontineMemberRepository;
    private final PresidentAccessChecker accessChecker;
    private final AuditService auditService;

    public ExtraordinaryContributionService(ExtraordinaryContributionRepository repository,
                                            ExtraContributionMemberRepository memberRepository,
                                            MemberRepository tontineMemberRepository,
                                            PresidentAccessChecker accessChecker,
                                            AuditService auditService) {
        this.repository = repository;
        this.memberRepository = memberRepository;
        this.tontineMemberRepository = tontineMemberRepository;
        this.accessChecker = accessChecker;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<ExtraordinaryContributionDto> list(UUID tontineId, UUID userId) {
        accessChecker.requirePresident(userId, tontineId);
        return repository.findByTontineIdOrderByCreatedAtDesc(tontineId).stream()
                .map(e -> ExtraordinaryContributionDto.from(e, loadMembers(e.getId())))
                .toList();
    }

    @Transactional
    public ExtraordinaryContributionDto create(UUID tontineId, UUID userId,
                                               CreateExtraordinaryContributionRequest req) {
        accessChecker.requirePresident(userId, tontineId);

        ExtraordinaryContribution e = new ExtraordinaryContribution();
        e.setTontineId(tontineId);
        e.setMotive(req.motive());
        e.setBeneficiaryMemberId(req.beneficiaryMemberId());
        e.setAmountPerMember(req.amountPerMember());
        e.setDueDate(req.dueDate());
        e.setExemptBeneficiary(req.exemptBeneficiary());
        e.setStatus(ExtraContributionStatus.COLLECTING);

        List<Member> activeMembers = tontineMemberRepository.findByTontineId(tontineId).stream()
                .filter(m -> m.getStatus() == MemberStatus.ACTIVE)
                .toList();

        if (req.beneficiaryMemberId() != null) {
            activeMembers.stream()
                    .filter(m -> m.getId().equals(req.beneficiaryMemberId()))
                    .findFirst()
                    .ifPresent(m -> e.setBeneficiaryFullName(m.getFirstName() + " " + m.getLastName()));
        }

        BigDecimal totalExpected = BigDecimal.ZERO;
        ExtraordinaryContribution saved = repository.save(e);

        for (Member m : activeMembers) {
            ExtraContributionMember entry = new ExtraContributionMember();
            entry.setExtraContribId(saved.getId());
            entry.setMemberId(m.getId());
            entry.setFullName(m.getFirstName() + " " + m.getLastName());
            boolean isBeneficiary = req.beneficiaryMemberId() != null
                    && m.getId().equals(req.beneficiaryMemberId());
            boolean exempt = isBeneficiary && req.exemptBeneficiary();
            entry.setExempted(exempt);
            BigDecimal expected = exempt ? BigDecimal.ZERO : req.amountPerMember();
            entry.setExpected(expected);
            entry.setPaid(BigDecimal.ZERO);
            memberRepository.save(entry);
            totalExpected = totalExpected.add(expected);
        }

        saved.setTotalExpected(totalExpected);
        saved = repository.save(saved);

        auditService.record(userId, "EXTRA_CONTRIB_CREATE", "ExtraordinaryContribution",
                saved.getId().toString(), tontineId,
                "{\"motive\":\"" + escape(req.motive()) + "\"}");
        return ExtraordinaryContributionDto.from(saved, loadMembers(saved.getId()));
    }

    @Transactional
    public ExtraordinaryContributionDto close(UUID id, UUID tontineId, UUID userId) {
        accessChecker.requirePresident(userId, tontineId);
        ExtraordinaryContribution e = loadInTontine(id, tontineId);
        if (e.getStatus() != ExtraContributionStatus.COLLECTING) {
            throw new ApiException("EXTRA_CONTRIB_INVALID_STATE",
                    "Seule une collecte en cours peut etre cloturee", HttpStatus.CONFLICT);
        }
        e.setStatus(ExtraContributionStatus.CLOSED);
        e.setClosedAt(Instant.now());
        ExtraordinaryContribution saved = repository.save(e);
        auditService.record(userId, "EXTRA_CONTRIB_CLOSE", "ExtraordinaryContribution",
                id.toString(), tontineId, null);
        return ExtraordinaryContributionDto.from(saved, loadMembers(saved.getId()));
    }

    @Transactional
    public ExtraordinaryContributionDto distribute(UUID id, UUID tontineId, UUID userId) {
        accessChecker.requirePresident(userId, tontineId);
        ExtraordinaryContribution e = loadInTontine(id, tontineId);
        if (e.getStatus() != ExtraContributionStatus.CLOSED) {
            throw new ApiException("EXTRA_CONTRIB_INVALID_STATE",
                    "Seule une collecte cloturee peut etre distribuee", HttpStatus.CONFLICT);
        }
        e.setStatus(ExtraContributionStatus.DISTRIBUTED);
        e.setDistributedAt(Instant.now());
        ExtraordinaryContribution saved = repository.save(e);
        auditService.record(userId, "EXTRA_CONTRIB_DISTRIBUTE", "ExtraordinaryContribution",
                id.toString(), tontineId, null);
        return ExtraordinaryContributionDto.from(saved, loadMembers(saved.getId()));
    }

    private ExtraordinaryContribution loadInTontine(UUID id, UUID tontineId) {
        ExtraordinaryContribution e = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ExtraordinaryContribution", id));
        if (!e.getTontineId().equals(tontineId)) {
            throw new ApiException("FORBIDDEN", "Tontine non concordante", HttpStatus.FORBIDDEN);
        }
        return e;
    }

    private List<ExtraContributionMemberDto> loadMembers(UUID extraId) {
        return memberRepository.findByExtraContribIdOrderByFullNameAsc(extraId).stream()
                .map(ExtraContributionMemberDto::from)
                .toList();
    }

    private String escape(String s) {
        return s == null ? "" : s.replace("\"", "\\\"");
    }
}
