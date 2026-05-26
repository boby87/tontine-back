package cm.ftg.tontine.secretary.archive.service;

import cm.ftg.tontine.audit.service.AuditService;
import cm.ftg.tontine.member.entity.Member;
import cm.ftg.tontine.secretary.archive.dto.ArchiveDocumentDto;
import cm.ftg.tontine.secretary.archive.dto.CreateArchiveRequest;
import cm.ftg.tontine.secretary.archive.entity.ArchiveDocument;
import cm.ftg.tontine.secretary.archive.enums.ArchiveDocumentType;
import cm.ftg.tontine.secretary.archive.repository.ArchiveDocumentRepository;
import cm.ftg.tontine.secretary.security.SecretaryAccessChecker;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ArchiveService {

    private final ArchiveDocumentRepository repository;
    private final SecretaryAccessChecker accessChecker;
    private final AuditService auditService;

    public ArchiveService(ArchiveDocumentRepository repository,
                          SecretaryAccessChecker accessChecker,
                          AuditService auditService) {
        this.repository = repository;
        this.accessChecker = accessChecker;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<ArchiveDocumentDto> list(UUID tontineId, UUID userId,
                                         ArchiveDocumentType type, Integer cycle) {
        accessChecker.requireSecretary(userId, tontineId);
        List<ArchiveDocument> items;
        if (type != null && cycle != null) {
            items = repository.findByTontineIdAndTypeAndCycleNumberOrderByUploadedAtDesc(tontineId, type, cycle);
        } else if (type != null) {
            items = repository.findByTontineIdAndTypeOrderByUploadedAtDesc(tontineId, type);
        } else if (cycle != null) {
            items = repository.findByTontineIdAndCycleNumberOrderByUploadedAtDesc(tontineId, cycle);
        } else {
            items = repository.findByTontineIdOrderByUploadedAtDesc(tontineId);
        }
        return items.stream().map(ArchiveDocumentDto::from).toList();
    }

    @Transactional
    public ArchiveDocumentDto create(UUID tontineId, UUID userId, CreateArchiveRequest req) {
        Member secretary = accessChecker.requireSecretary(userId, tontineId);
        ArchiveDocument d = new ArchiveDocument();
        d.setTontineId(tontineId);
        d.setType(req.type());
        d.setTitle(req.title());
        d.setDescription(req.description());
        d.setFileName(req.fileName());
        d.setFileSize(req.fileSize());
        d.setVisibility(req.visibility());
        d.setCycleNumber(req.cycleNumber());
        d.setSessionNumber(req.sessionNumber());
        d.setUploadedByUserId(userId);
        d.setUploadedByFullName(buildFullName(secretary));
        d.setTags(req.tags() == null ? new ArrayList<>() : new ArrayList<>(req.tags()));
        ArchiveDocument saved = repository.save(d);
        auditService.record(userId, "ARCHIVE_CREATE", "ArchiveDocument", saved.getId().toString(),
                tontineId, "{\"type\":\"" + req.type().name() + "\"}");
        return ArchiveDocumentDto.from(saved);
    }

    private String buildFullName(Member m) {
        String first = m.getFirstName() == null ? "" : m.getFirstName();
        String last = m.getLastName() == null ? "" : m.getLastName();
        String full = (first + " " + last).trim();
        return full.isEmpty() ? m.getMatricule() : full;
    }
}
