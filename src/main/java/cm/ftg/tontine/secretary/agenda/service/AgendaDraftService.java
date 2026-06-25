package cm.ftg.tontine.secretary.agenda.service;

import cm.ftg.tontine.audit.service.AuditService;
import cm.ftg.tontine.common.exception.ApiException;
import cm.ftg.tontine.common.exception.ResourceNotFoundException;
import cm.ftg.tontine.member.entity.Member;
import cm.ftg.tontine.member.repository.MemberRepository;
import cm.ftg.tontine.secretary.agenda.dto.AgendaDraftDto;
import cm.ftg.tontine.secretary.agenda.dto.AgendaDraftItemDto;
import cm.ftg.tontine.secretary.agenda.dto.CreateAgendaDraftItemRequest;
import cm.ftg.tontine.secretary.agenda.dto.CreateAgendaDraftRequest;
import cm.ftg.tontine.secretary.agenda.entity.AgendaDraft;
import cm.ftg.tontine.secretary.agenda.entity.AgendaDraftItem;
import cm.ftg.tontine.secretary.agenda.enums.AgendaDraftStatus;
import cm.ftg.tontine.secretary.agenda.repository.AgendaDraftItemRepository;
import cm.ftg.tontine.secretary.agenda.repository.AgendaDraftRepository;
import cm.ftg.tontine.secretary.security.SecretaryAccessChecker;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AgendaDraftService {

    private final AgendaDraftRepository draftRepository;
    private final AgendaDraftItemRepository itemRepository;
    private final MemberRepository memberRepository;
    private final SecretaryAccessChecker accessChecker;
    private final AuditService auditService;

    public AgendaDraftService(AgendaDraftRepository draftRepository,
                              AgendaDraftItemRepository itemRepository,
                              MemberRepository memberRepository,
                              SecretaryAccessChecker accessChecker,
                              AuditService auditService) {
        this.draftRepository = draftRepository;
        this.itemRepository = itemRepository;
        this.memberRepository = memberRepository;
        this.accessChecker = accessChecker;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public int nextSessionNumber(UUID tontineId, UUID userId) {
        accessChecker.requireSecretary(userId, tontineId);
        return draftRepository.findNextSessionNumber(tontineId);
    }

    @Transactional(readOnly = true)
    public List<AgendaDraftDto> list(UUID tontineId, UUID userId) {
        accessChecker.requireSecretary(userId, tontineId);
        return draftRepository.findByTontineIdOrderByCreatedAtDesc(tontineId).stream()
                .map(AgendaDraftDto::fromSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public AgendaDraftDto findById(UUID id, UUID tontineId, UUID userId) {
        accessChecker.requireSecretary(userId, tontineId);
        AgendaDraft d = loadInTontine(id, tontineId);
        return buildDetailed(d);
    }

    @Transactional
    public AgendaDraftDto create(UUID tontineId, UUID userId, CreateAgendaDraftRequest req) {
        accessChecker.requireSecretary(userId, tontineId);

        AgendaDraft draft = new AgendaDraft();
        draft.setTontineId(tontineId);
        draft.setSessionId(req.sessionId());
        draft.setSessionNumber(req.sessionNumber());
        draft.setScheduledAt(req.scheduledAt());
        draft.setLocation(req.location());
        draft.setStatus(AgendaDraftStatus.DRAFT);

        if (req.beneficiaryMemberId() != null) {
            Member b = memberRepository.findById(req.beneficiaryMemberId())
                    .orElseThrow(() -> new ResourceNotFoundException("Member", req.beneficiaryMemberId()));
            if (!b.getTontineId().equals(tontineId)) {
                throw new ApiException("FORBIDDEN", "Beneficiaire hors tontine", HttpStatus.FORBIDDEN);
            }
            draft.setBeneficiaryMemberId(b.getId());
            draft.setBeneficiaryFullName(b.getFirstName() + " " + b.getLastName());
        }

        AgendaDraft saved = draftRepository.save(draft);

        int order = 1;
        for (CreateAgendaDraftItemRequest itemReq : req.items()) {
            AgendaDraftItem item = new AgendaDraftItem();
            item.setAgendaDraftId(saved.getId());
            item.setOrderIdx(order++);
            item.setTitle(itemReq.title());
            item.setDescription(itemReq.description());
            item.setStandard(itemReq.isStandard());
            item.setEstimatedDurationMin(itemReq.estimatedDurationMin());
            itemRepository.save(item);
        }

        auditService.record(userId, "AGENDA_DRAFT_CREATE", "AgendaDraft",
                saved.getId().toString(), tontineId,
                "{\"sessionId\":\"" + req.sessionId() + "\"}");
        return buildDetailed(saved);
    }

    @Transactional
    public AgendaDraftDto submit(UUID id, UUID tontineId, UUID userId) {
        accessChecker.requireSecretary(userId, tontineId);
        AgendaDraft d = loadInTontine(id, tontineId);
        if (d.getStatus() != AgendaDraftStatus.DRAFT
                && d.getStatus() != AgendaDraftStatus.CHANGES_REQUESTED) {
            throw new ApiException("AGENDA_INVALID_STATE",
                    "Le brouillon ne peut etre soumis que depuis DRAFT ou CHANGES_REQUESTED",
                    HttpStatus.CONFLICT);
        }
        d.setStatus(AgendaDraftStatus.SUBMITTED_TO_PRESIDENT);
        d.setSubmittedAt(Instant.now());
        AgendaDraft saved = draftRepository.save(d);
        auditService.record(userId, "AGENDA_DRAFT_SUBMIT", "AgendaDraft",
                id.toString(), tontineId, null);
        return buildDetailed(saved);
    }

    private AgendaDraft loadInTontine(UUID id, UUID tontineId) {
        AgendaDraft d = draftRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AgendaDraft", id));
        if (!d.getTontineId().equals(tontineId)) {
            throw new ApiException("FORBIDDEN", "Tontine non concordante", HttpStatus.FORBIDDEN);
        }
        return d;
    }

    private AgendaDraftDto buildDetailed(AgendaDraft d) {
        List<AgendaDraftItemDto> items = itemRepository
                .findByAgendaDraftIdOrderByOrderIdxAsc(d.getId())
                .stream()
                .map(AgendaDraftItemDto::from)
                .toList();
        return AgendaDraftDto.from(d, items);
    }
}
