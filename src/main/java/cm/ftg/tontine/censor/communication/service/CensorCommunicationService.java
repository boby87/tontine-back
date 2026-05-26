package cm.ftg.tontine.censor.communication.service;

import cm.ftg.tontine.audit.service.AuditService;
import cm.ftg.tontine.censor.communication.dto.CensorCommunicationDto;
import cm.ftg.tontine.censor.communication.dto.CreateCommunicationRequest;
import cm.ftg.tontine.censor.communication.entity.CensorCommunication;
import cm.ftg.tontine.censor.communication.repository.CensorCommunicationRepository;
import cm.ftg.tontine.censor.security.CensorAccessChecker;
import cm.ftg.tontine.member.entity.Member;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CensorCommunicationService {

    private final CensorCommunicationRepository repository;
    private final CensorAccessChecker accessChecker;
    private final AuditService auditService;

    public CensorCommunicationService(CensorCommunicationRepository repository,
                                      CensorAccessChecker accessChecker,
                                      AuditService auditService) {
        this.repository = repository;
        this.accessChecker = accessChecker;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<CensorCommunicationDto> list(UUID tontineId, UUID userId) {
        accessChecker.requireCensor(userId, tontineId);
        return repository.findByTontineIdOrderBySentAtDesc(tontineId).stream()
                .map(CensorCommunicationDto::from)
                .toList();
    }

    @Transactional
    public CensorCommunicationDto create(UUID tontineId, UUID userId, CreateCommunicationRequest req) {
        Member censor = accessChecker.requireCensor(userId, tontineId);
        CensorCommunication c = new CensorCommunication();
        c.setTontineId(tontineId);
        c.setKind(req.kind());
        c.setSubject(req.subject());
        c.setBody(req.body());
        c.setChannels(new HashSet<>(req.channels()));
        c.setRecipientMemberIds(new ArrayList<>(req.recipientMemberIds()));
        c.setRelatedSanctionIds(req.relatedSanctionIds() == null ? new ArrayList<>() : new ArrayList<>(req.relatedSanctionIds()));
        c.setSentByUserId(userId);
        c.setSentByFullName(fullName(censor));
        c.setRecipientsCount(req.recipientMemberIds().size());
        CensorCommunication saved = repository.save(c);
        auditService.record(userId, "CENSOR_COMMUNICATION_SEND", "CensorCommunication",
                saved.getId().toString(), tontineId,
                "{\"kind\":\"" + req.kind() + "\",\"recipientsCount\":" + req.recipientMemberIds().size() + "}");
        return CensorCommunicationDto.from(saved);
    }

    private String fullName(Member m) {
        return (m.getFirstName() == null ? "" : m.getFirstName()) + " "
                + (m.getLastName() == null ? "" : m.getLastName());
    }
}
