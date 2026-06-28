package cm.ftg.tontine.secretary.agenda.service;

import cm.ftg.tontine.audit.service.AuditService;
import cm.ftg.tontine.common.enums.MemberStatus;
import cm.ftg.tontine.common.enums.UserRole;
import cm.ftg.tontine.common.exception.ApiException;
import cm.ftg.tontine.common.exception.ResourceNotFoundException;
import cm.ftg.tontine.member.entity.Member;
import cm.ftg.tontine.member.repository.MemberRepository;
import cm.ftg.tontine.notification.enums.NotificationCategory;
import cm.ftg.tontine.notification.enums.NotificationKind;
import cm.ftg.tontine.notification.service.NotificationService;
import cm.ftg.tontine.president.security.PresidentAccessChecker;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AgendaDraftService {

    private static final Logger log = LoggerFactory.getLogger(AgendaDraftService.class);

    private final AgendaDraftRepository draftRepository;
    private final AgendaDraftItemRepository itemRepository;
    private final MemberRepository memberRepository;
    private final SecretaryAccessChecker accessChecker;
    private final PresidentAccessChecker presidentAccessChecker;
    private final AuditService auditService;
    private final NotificationService notificationService;

    public AgendaDraftService(AgendaDraftRepository draftRepository,
                              AgendaDraftItemRepository itemRepository,
                              MemberRepository memberRepository,
                              SecretaryAccessChecker accessChecker,
                              PresidentAccessChecker presidentAccessChecker,
                              AuditService auditService,
                              NotificationService notificationService) {
        this.draftRepository = draftRepository;
        this.itemRepository = itemRepository;
        this.memberRepository = memberRepository;
        this.accessChecker = accessChecker;
        this.presidentAccessChecker = presidentAccessChecker;
        this.auditService = auditService;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public int nextSessionNumber(UUID tontineId, UUID userId) {
        accessChecker.requireSecretary(userId, tontineId);
        return draftRepository.findNextSessionNumber(tontineId);
    }

    @Transactional(readOnly = true)
    public List<AgendaDraftDto> list(UUID tontineId, UUID userId, UUID sessionId) {
        accessChecker.requireSecretary(userId, tontineId);
        List<AgendaDraft> drafts = sessionId != null
                ? draftRepository.findByTontineIdAndSessionIdOrderByCreatedAtDesc(tontineId, sessionId)
                : draftRepository.findByTontineIdOrderByCreatedAtDesc(tontineId);
        return drafts.stream()
                .map(this::buildDetailed)
                .toList();
    }

    @Transactional(readOnly = true)
    public AgendaDraftDto findById(UUID id, UUID tontineId, UUID userId) {
        accessChecker.requireSecretary(userId, tontineId);
        AgendaDraft d = loadInTontine(id, tontineId);
        return buildDetailed(d);
    }

    // ── President-facing ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<AgendaDraftDto> listPending(UUID tontineId, UUID userId, UUID sessionId) {
        presidentAccessChecker.requirePresident(userId, tontineId);
        List<AgendaDraft> drafts = sessionId != null
                ? draftRepository.findByTontineIdAndStatusAndSessionIdOrderByCreatedAtDesc(
                        tontineId, AgendaDraftStatus.SUBMITTED_TO_PRESIDENT, sessionId)
                : draftRepository.findByTontineIdAndStatusOrderByCreatedAtDesc(
                        tontineId, AgendaDraftStatus.SUBMITTED_TO_PRESIDENT);
        return drafts.stream().map(this::buildDetailed).toList();
    }

    @Transactional
    public AgendaDraftDto approve(UUID id, UUID tontineId, UUID userId) {
        presidentAccessChecker.requirePresident(userId, tontineId);
        AgendaDraft d = loadInTontine(id, tontineId);
        if (d.getStatus() != AgendaDraftStatus.SUBMITTED_TO_PRESIDENT) {
            throw new ApiException("AGENDA_INVALID_STATE",
                    "Seul un ODJ SUBMITTED_TO_PRESIDENT peut etre approuve",
                    HttpStatus.CONFLICT);
        }
        d.setStatus(AgendaDraftStatus.APPROVED);
        d.setApprovedAt(Instant.now());
        AgendaDraft saved = draftRepository.save(d);
        auditService.record(userId, "AGENDA_DRAFT_APPROVE", "AgendaDraft",
                id.toString(), tontineId, null);
        notifySecretaries(tontineId, d, NotificationKind.SUCCESS,
                "Ordre du jour approuve",
                "L'ordre du jour de la Seance #" + d.getSessionNumber()
                        + " a ete approuve par le President.",
                "/secretary/sessions/" + d.getSessionId());
        if (d.getSessionId() != null) {
            notifyMembers(tontineId, d);
        }
        return buildDetailed(saved);
    }

    @Transactional
    public AgendaDraftDto requestChanges(UUID id, UUID tontineId, UUID userId, String comment) {
        presidentAccessChecker.requirePresident(userId, tontineId);
        AgendaDraft d = loadInTontine(id, tontineId);
        if (d.getStatus() != AgendaDraftStatus.SUBMITTED_TO_PRESIDENT) {
            throw new ApiException("AGENDA_INVALID_STATE",
                    "Seul un ODJ SUBMITTED_TO_PRESIDENT peut faire l objet de demande de modification",
                    HttpStatus.CONFLICT);
        }
        d.setStatus(AgendaDraftStatus.CHANGES_REQUESTED);
        d.setPresidentComment(comment);
        AgendaDraft saved = draftRepository.save(d);
        auditService.record(userId, "AGENDA_CHANGES_REQUESTED", "AgendaDraft",
                id.toString(), tontineId, "{\"comment\":\"" + comment.replace("\"", "\\\"") + "\"}");
        notifySecretaries(tontineId, d, NotificationKind.WARNING,
                "Modifications demandees pour l'ODJ",
                "Le President a demande des modifications pour l'ordre du jour de la Seance #"
                        + d.getSessionNumber() + ". Commentaire : " + comment,
                "/secretary/sessions/" + d.getSessionId());
        return buildDetailed(saved);
    }

    // ── Secretary actions ────────────────────────────────────────────────────

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
    public AgendaDraftDto updateItems(UUID id, UUID tontineId, UUID userId,
                                      List<CreateAgendaDraftItemRequest> items) {
        accessChecker.requireSecretary(userId, tontineId);
        AgendaDraft d = loadInTontine(id, tontineId);
        if (d.getStatus() != AgendaDraftStatus.DRAFT
                && d.getStatus() != AgendaDraftStatus.CHANGES_REQUESTED) {
            throw new ApiException("AGENDA_INVALID_STATE",
                    "Les points ne peuvent etre modifies qu en DRAFT ou CHANGES_REQUESTED",
                    HttpStatus.CONFLICT);
        }
        itemRepository.deleteByAgendaDraftId(id);
        int order = 1;
        for (CreateAgendaDraftItemRequest itemReq : items) {
            AgendaDraftItem item = new AgendaDraftItem();
            item.setAgendaDraftId(id);
            item.setOrderIdx(order++);
            item.setTitle(itemReq.title());
            item.setDescription(itemReq.description());
            item.setStandard(itemReq.isStandard());
            item.setEstimatedDurationMin(itemReq.estimatedDurationMin());
            itemRepository.save(item);
        }
        auditService.record(userId, "AGENDA_ITEMS_UPDATE", "AgendaDraft",
                id.toString(), tontineId, "{\"count\":" + items.size() + "}");
        return buildDetailed(d);
    }

    @Transactional
    public void delete(UUID id, UUID tontineId, UUID userId) {
        accessChecker.requireSecretary(userId, tontineId);
        AgendaDraft d = loadInTontine(id, tontineId);
        if (d.getStatus() != AgendaDraftStatus.DRAFT
                && d.getStatus() != AgendaDraftStatus.CHANGES_REQUESTED) {
            throw new ApiException("AGENDA_INVALID_STATE",
                    "Seul un brouillon DRAFT ou CHANGES_REQUESTED peut etre supprime",
                    HttpStatus.CONFLICT);
        }
        d.setDeletedAt(Instant.now());
        draftRepository.save(d);
        auditService.record(userId, "AGENDA_DRAFT_DELETE", "AgendaDraft",
                id.toString(), tontineId, null);
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
        // Notify president(s)
        List<Member> presidents = memberRepository.findByTontineIdAndRole(tontineId, UserRole.PRESIDENT);
        for (Member p : presidents) {
            try {
                notificationService.publish(p.getUserId(), tontineId,
                        NotificationKind.INFO, NotificationCategory.AGENDA,
                        "Ordre du jour a approuver",
                        "L'ordre du jour de la Seance #" + d.getSessionNumber()
                                + " vous a ete soumis pour approbation.",
                        "/president/sessions/" + d.getSessionId());
            } catch (Exception ex) {
                log.warn("Notification president failed: {}", ex.getMessage());
            }
        }
        return buildDetailed(saved);
    }

    // ── Shared helpers ───────────────────────────────────────────────────────

    private void notifySecretaries(UUID tontineId, AgendaDraft d,
                                   NotificationKind kind, String title,
                                   String message, String link) {
        List<Member> secretaries = memberRepository.findByTontineIdAndRole(
                tontineId, UserRole.SECRETARY);
        for (Member s : secretaries) {
            try {
                notificationService.publish(s.getUserId(), tontineId,
                        kind, NotificationCategory.AGENDA, title, message, link);
            } catch (Exception ex) {
                log.warn("Notification secretary failed: {}", ex.getMessage());
            }
        }
    }

    private void notifyMembers(UUID tontineId, AgendaDraft d) {
        String title = "Ordre du jour disponible — Seance #" + d.getSessionNumber();
        String message = "L'ordre du jour de la Seance #" + d.getSessionNumber()
                + " a ete approuve. Consultez-le avant la seance.";
        String link = "/member/sessions/" + d.getSessionId();
        memberRepository.findByTontineId(tontineId).stream()
                .filter(m -> m.getStatus() == MemberStatus.ACTIVE)
                .forEach(m -> {
                    try {
                        notificationService.publish(m.getUserId(), tontineId,
                                NotificationKind.INFO, NotificationCategory.AGENDA,
                                title, message, link);
                    } catch (Exception ex) {
                        log.warn("Notification member failed: {}", ex.getMessage());
                    }
                });
    }

    private AgendaDraft loadInTontine(UUID id, UUID tontineId) {
        AgendaDraft d = draftRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AgendaDraft", id));
        if (!d.getTontineId().equals(tontineId)) {
            throw new ApiException("FORBIDDEN", "Tontine non concordante", HttpStatus.FORBIDDEN);
        }
        return d;
    }

    AgendaDraftDto buildDetailed(AgendaDraft d) {
        List<AgendaDraftItemDto> items = itemRepository
                .findByAgendaDraftIdOrderByOrderIdxAsc(d.getId())
                .stream()
                .map(AgendaDraftItemDto::from)
                .toList();
        return AgendaDraftDto.from(d, items);
    }
}
