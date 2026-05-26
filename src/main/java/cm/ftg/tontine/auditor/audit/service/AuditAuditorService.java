package cm.ftg.tontine.auditor.audit.service;

import cm.ftg.tontine.audit.service.AuditService;
import cm.ftg.tontine.auditor.audit.dto.AuditDto;
import cm.ftg.tontine.auditor.audit.dto.AuditFindingDto;
import cm.ftg.tontine.auditor.audit.dto.CreateAuditRequest;
import cm.ftg.tontine.auditor.audit.entity.Audit;
import cm.ftg.tontine.auditor.audit.entity.AuditFindingEntry;
import cm.ftg.tontine.auditor.audit.repository.AuditFindingEntryRepository;
import cm.ftg.tontine.auditor.audit.repository.AuditRepository;
import cm.ftg.tontine.auditor.security.AuditorAccessChecker;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditAuditorService {

    private final AuditRepository auditRepository;
    private final AuditFindingEntryRepository findingRepository;
    private final AuditorAccessChecker accessChecker;
    private final AuditService auditService;

    public AuditAuditorService(AuditRepository auditRepository,
                               AuditFindingEntryRepository findingRepository,
                               AuditorAccessChecker accessChecker,
                               AuditService auditService) {
        this.auditRepository = auditRepository;
        this.findingRepository = findingRepository;
        this.accessChecker = accessChecker;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<AuditDto> list(UUID tontineId, UUID userId) {
        accessChecker.requireAuditor(userId, tontineId);
        List<Audit> audits = auditRepository.findByTontineIdOrderByCreatedAtDesc(tontineId);
        if (audits.isEmpty()) {
            return List.of();
        }
        List<UUID> ids = audits.stream().map(Audit::getId).toList();
        Map<UUID, List<AuditFindingDto>> grouped = new HashMap<>();
        for (AuditFindingEntry f : findingRepository.findByAuditIdInOrderByOrderIdxAsc(ids)) {
            grouped.computeIfAbsent(f.getAuditId(), k -> new ArrayList<>())
                    .add(AuditFindingDto.from(f));
        }
        return audits.stream()
                .map(a -> AuditDto.from(a, grouped.getOrDefault(a.getId(), List.of())))
                .toList();
    }

    @Transactional
    public AuditDto create(UUID tontineId, UUID userId, String fullName, CreateAuditRequest req) {
        accessChecker.requireAuditor(userId, tontineId);
        Audit a = new Audit();
        a.setTontineId(tontineId);
        a.setScope(req.scope());
        a.setPeriodFrom(req.periodFrom());
        a.setPeriodTo(req.periodTo());
        a.setOverallFinding(req.overallFinding());
        a.setObservations(req.observations());
        a.setCreatedByUserId(userId);
        a.setCreatedByFullName(fullName);
        Audit saved = auditRepository.save(a);

        List<AuditFindingDto> findingDtos = new ArrayList<>();
        int idx = 0;
        for (CreateAuditRequest.FindingInput in : req.findings()) {
            AuditFindingEntry f = new AuditFindingEntry();
            f.setAuditId(saved.getId());
            f.setArea(in.area());
            f.setFinding(in.finding());
            f.setDescription(in.description());
            f.setOrderIdx(idx++);
            findingDtos.add(AuditFindingDto.from(findingRepository.save(f)));
        }

        auditService.record(userId, "AUDIT_CREATE", "Audit", saved.getId().toString(), tontineId,
                "{\"scope\":\"" + req.scope() + "\",\"overall\":\"" + req.overallFinding() + "\"}");
        return AuditDto.from(saved, findingDtos);
    }
}
