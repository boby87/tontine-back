package cm.ftg.tontine.auditor.certification.service;

import cm.ftg.tontine.audit.service.AuditService;
import cm.ftg.tontine.auditor.certification.dto.CertificationDto;
import cm.ftg.tontine.auditor.certification.dto.IssueCertificationRequest;
import cm.ftg.tontine.auditor.certification.entity.Certification;
import cm.ftg.tontine.auditor.certification.repository.CertificationRepository;
import cm.ftg.tontine.auditor.security.AuditorAccessChecker;
import cm.ftg.tontine.member.entity.Member;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CertificationService {

    private final CertificationRepository repository;
    private final AuditorAccessChecker accessChecker;
    private final AuditService auditService;

    public CertificationService(CertificationRepository repository,
                                AuditorAccessChecker accessChecker,
                                AuditService auditService) {
        this.repository = repository;
        this.accessChecker = accessChecker;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<CertificationDto> list(UUID tontineId, UUID userId) {
        accessChecker.requireAuditor(userId, tontineId);
        return repository.findByTontineIdOrderByIssuedAtDesc(tontineId).stream()
                .map(CertificationDto::from)
                .toList();
    }

    @Transactional
    public CertificationDto issue(UUID tontineId, UUID userId, IssueCertificationRequest req) {
        Member auditor = accessChecker.requireAuditor(userId, tontineId);
        // OTP stub already validated via @Pattern on the request.
        Certification c = new Certification();
        c.setTontineId(tontineId);
        c.setScope(req.scope());
        c.setPeriodLabel(req.periodLabel());
        c.setDecision(req.decision());
        c.setReserves(req.reserves());
        c.setIssuedByUserId(userId);
        c.setIssuedByFullName(auditor.getFirstName() + " " + auditor.getLastName());
        Certification saved = repository.save(c);
        auditService.record(userId, "CERTIFICATION_ISSUE", "Certification",
                saved.getId().toString(), tontineId,
                "{\"scope\":\"" + req.scope().name()
                        + "\",\"decision\":\"" + req.decision().name() + "\"}");
        return CertificationDto.from(saved);
    }
}
