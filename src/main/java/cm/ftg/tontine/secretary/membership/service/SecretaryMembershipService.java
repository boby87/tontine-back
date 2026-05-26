package cm.ftg.tontine.secretary.membership.service;

import cm.ftg.tontine.audit.service.AuditService;
import cm.ftg.tontine.common.exception.ApiException;
import cm.ftg.tontine.common.exception.ResourceNotFoundException;
import cm.ftg.tontine.president.membership.dto.MembershipFileDto;
import cm.ftg.tontine.president.membership.entity.MembershipFile;
import cm.ftg.tontine.president.membership.enums.MembershipFileKind;
import cm.ftg.tontine.president.membership.enums.MembershipFileStatus;
import cm.ftg.tontine.president.membership.repository.MembershipFileRepository;
import cm.ftg.tontine.secretary.membership.dto.SecretaryMembershipReviewRequest;
import cm.ftg.tontine.secretary.membership.enums.SecretaryMembershipDecision;
import cm.ftg.tontine.secretary.security.SecretaryAccessChecker;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SecretaryMembershipService {

    private static final Set<MembershipFileStatus> PRE_PRESIDENT_STATES = Set.of(
            MembershipFileStatus.SUBMITTED,
            MembershipFileStatus.BUREAU_REVIEW,
            MembershipFileStatus.ASSEMBLY_VOTE_PENDING,
            MembershipFileStatus.ASSEMBLY_APPROVED,
            MembershipFileStatus.ASSEMBLY_REJECTED
    );

    private final MembershipFileRepository repository;
    private final SecretaryAccessChecker accessChecker;
    private final AuditService auditService;

    public SecretaryMembershipService(MembershipFileRepository repository,
                                      SecretaryAccessChecker accessChecker,
                                      AuditService auditService) {
        this.repository = repository;
        this.accessChecker = accessChecker;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<MembershipFileDto> list(UUID tontineId, UUID userId, MembershipFileKind kind) {
        accessChecker.requireSecretary(userId, tontineId);
        List<MembershipFile> items = (kind == null)
                ? repository.findByTontineIdAndStatusInOrderBySubmittedAtDesc(tontineId, PRE_PRESIDENT_STATES)
                : repository.findByTontineIdAndKindAndStatusInOrderBySubmittedAtDesc(
                        tontineId, kind, PRE_PRESIDENT_STATES);
        return items.stream().map(MembershipFileDto::from).toList();
    }

    @Transactional
    public MembershipFileDto review(UUID id, UUID tontineId, UUID userId,
                                    SecretaryMembershipReviewRequest req) {
        accessChecker.requireSecretary(userId, tontineId);
        MembershipFile m = loadInTontine(id, tontineId);
        if (!PRE_PRESIDENT_STATES.contains(m.getStatus())) {
            throw new ApiException("MEMBERSHIP_INVALID_STATE",
                    "Le dossier n'est plus dans un etat permettant une revue secretaire",
                    HttpStatus.CONFLICT);
        }
        if (req.decision() == SecretaryMembershipDecision.FORWARD) {
            m.setStatus(MembershipFileStatus.PRESIDENT_REVIEW);
            m.setBureauReviewedAt(Instant.now());
        } else {
            m.setStatus(MembershipFileStatus.REJECTED);
        }
        if (req.comment() != null && !req.comment().isBlank()) {
            m.setPresidentDecisionComment(req.comment());
        }
        MembershipFile saved = repository.save(m);
        auditService.record(userId, "MEMBERSHIP_SECRETARY_REVIEW", "MembershipFile", id.toString(),
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
