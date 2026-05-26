package cm.ftg.tontine.auditor.balancereview.service;

import cm.ftg.tontine.audit.service.AuditService;
import cm.ftg.tontine.auditor.balancereview.dto.CreateBalanceReviewRequest;
import cm.ftg.tontine.auditor.balancereview.dto.SessionBalanceReviewDto;
import cm.ftg.tontine.auditor.balancereview.entity.SessionBalanceReview;
import cm.ftg.tontine.auditor.balancereview.repository.SessionBalanceReviewRepository;
import cm.ftg.tontine.auditor.security.AuditorAccessChecker;
import cm.ftg.tontine.common.exception.ApiException;
import cm.ftg.tontine.common.exception.ResourceNotFoundException;
import cm.ftg.tontine.member.entity.Member;
import cm.ftg.tontine.president.session.entity.Session;
import cm.ftg.tontine.president.session.repository.SessionRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditorBalanceReviewService {

    private final SessionBalanceReviewRepository repository;
    private final SessionRepository sessionRepository;
    private final AuditorAccessChecker accessChecker;
    private final AuditService auditService;

    public AuditorBalanceReviewService(SessionBalanceReviewRepository repository,
                                       SessionRepository sessionRepository,
                                       AuditorAccessChecker accessChecker,
                                       AuditService auditService) {
        this.repository = repository;
        this.sessionRepository = sessionRepository;
        this.accessChecker = accessChecker;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<SessionBalanceReviewDto> list(UUID tontineId, UUID userId) {
        accessChecker.requireAuditor(userId, tontineId);
        return repository.findByTontineIdOrderByReviewedAtDesc(tontineId).stream()
                .map(SessionBalanceReviewDto::from)
                .toList();
    }

    @Transactional
    public SessionBalanceReviewDto create(UUID tontineId, UUID userId, CreateBalanceReviewRequest req) {
        Member auditor = accessChecker.requireAuditor(userId, tontineId);
        Session session = sessionRepository.findById(req.sessionId())
                .orElseThrow(() -> new ResourceNotFoundException("Session", req.sessionId()));
        if (!session.getTontineId().equals(tontineId)) {
            throw new ApiException("FORBIDDEN", "Tontine non concordante", HttpStatus.FORBIDDEN);
        }
        if (repository.existsBySessionId(req.sessionId())) {
            throw new ApiException("BALANCE_REVIEW_ALREADY_EXISTS",
                    "Une revue existe deja pour cette session", HttpStatus.CONFLICT);
        }
        SessionBalanceReview review = new SessionBalanceReview();
        review.setTontineId(tontineId);
        review.setSessionId(session.getId());
        review.setSessionNumber(session.getNumber());
        review.setDecision(req.decision());
        review.setObservations(req.observations());
        review.setReserves(req.reserves());
        review.setReviewedByUserId(userId);
        review.setReviewedByFullName(auditor.getFirstName() + " " + auditor.getLastName());
        SessionBalanceReview saved = repository.save(review);
        auditService.record(userId, "BALANCE_REVIEW_CREATE", "SessionBalanceReview",
                saved.getId().toString(), tontineId,
                "{\"sessionId\":\"" + session.getId() + "\",\"decision\":\""
                        + req.decision().name() + "\"}");
        return SessionBalanceReviewDto.from(saved);
    }
}
