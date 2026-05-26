package cm.ftg.tontine.president.membership.service;

import cm.ftg.tontine.audit.service.AuditService;
import cm.ftg.tontine.common.exception.ApiException;
import cm.ftg.tontine.common.exception.ResourceNotFoundException;
import cm.ftg.tontine.president.membership.dto.MembershipDecisionRequest;
import cm.ftg.tontine.president.membership.dto.MembershipFileDto;
import cm.ftg.tontine.president.membership.entity.MembershipFile;
import cm.ftg.tontine.president.membership.enums.MembershipDecision;
import cm.ftg.tontine.president.membership.enums.MembershipFileKind;
import cm.ftg.tontine.president.membership.enums.MembershipFileStatus;
import cm.ftg.tontine.president.membership.repository.MembershipFileRepository;
import cm.ftg.tontine.president.security.PresidentAccessChecker;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MembershipFileService {

    private static final Set<MembershipFileStatus> DECIDABLE = Set.of(
            MembershipFileStatus.PRESIDENT_REVIEW,
            MembershipFileStatus.SUBMITTED,
            MembershipFileStatus.BUREAU_REVIEW,
            MembershipFileStatus.ASSEMBLY_APPROVED,
            MembershipFileStatus.ASSEMBLY_REJECTED
    );

    private final MembershipFileRepository repository;
    private final PresidentAccessChecker accessChecker;
    private final AuditService auditService;

    public MembershipFileService(MembershipFileRepository repository,
                                 PresidentAccessChecker accessChecker,
                                 AuditService auditService) {
        this.repository = repository;
        this.accessChecker = accessChecker;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<MembershipFileDto> list(UUID tontineId, UUID userId, MembershipFileKind kind) {
        accessChecker.requirePresident(userId, tontineId);
        List<MembershipFile> items = (kind == null)
                ? repository.findByTontineIdOrderBySubmittedAtDesc(tontineId)
                : repository.findByTontineIdAndKindOrderBySubmittedAtDesc(tontineId, kind);
        return items.stream().map(MembershipFileDto::from).toList();
    }

    @Transactional(readOnly = true)
    public MembershipFileDto findById(UUID id, UUID tontineId, UUID userId) {
        accessChecker.requirePresident(userId, tontineId);
        return MembershipFileDto.from(loadInTontine(id, tontineId));
    }

    @Transactional
    public MembershipFileDto decide(UUID id, UUID tontineId, UUID userId, MembershipDecisionRequest req) {
        accessChecker.requirePresident(userId, tontineId);
        MembershipFile m = loadInTontine(id, tontineId);
        if (!DECIDABLE.contains(m.getStatus())) {
            throw new ApiException("MEMBERSHIP_INVALID_STATE",
                    "Le dossier n'est pas dans un etat permettant une decision presidentielle",
                    HttpStatus.CONFLICT);
        }
        m.setStatus(req.decision() == MembershipDecision.APPROVE
                ? MembershipFileStatus.APPROVED
                : MembershipFileStatus.REJECTED);
        m.setPresidentDecidedAt(Instant.now());
        m.setPresidentDecisionComment(req.comment());
        MembershipFile saved = repository.save(m);
        auditService.record(userId, "MEMBERSHIP_DECIDE", "MembershipFile", id.toString(),
                tontineId, "{\"decision\":\"" + req.decision().name() + "\"}");
        return MembershipFileDto.from(saved);
    }

    private MembershipFile loadInTontine(UUID id, UUID tontineId) {
        MembershipFile m = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MembershipFile", id));
        if (!m.getTontineId().equals(tontineId)) {
            throw new ApiException("FORBIDDEN", "Tontine non concordante", HttpStatus.FORBIDDEN);
        }
        return m;
    }
}
