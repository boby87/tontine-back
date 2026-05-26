package cm.ftg.tontine.secretary.minutes.service;

import cm.ftg.tontine.audit.service.AuditService;
import cm.ftg.tontine.common.exception.ApiException;
import cm.ftg.tontine.common.exception.ResourceNotFoundException;
import cm.ftg.tontine.secretary.minutes.dto.MinutesDraftDto;
import cm.ftg.tontine.secretary.minutes.dto.MinutesSectionDto;
import cm.ftg.tontine.secretary.minutes.dto.UpdateMinutesSectionsRequest;
import cm.ftg.tontine.secretary.minutes.entity.MinutesDraft;
import cm.ftg.tontine.secretary.minutes.entity.MinutesSectionEntity;
import cm.ftg.tontine.secretary.minutes.enums.MinutesStatus;
import cm.ftg.tontine.secretary.minutes.repository.MinutesDraftRepository;
import cm.ftg.tontine.secretary.minutes.repository.MinutesSectionRepository;
import cm.ftg.tontine.secretary.security.SecretaryAccessChecker;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MinutesService {

    private static final Set<MinutesStatus> EDITABLE = Set.of(
            MinutesStatus.DRAFT, MinutesStatus.CHANGES_REQUESTED, MinutesStatus.SECRETARY_SIGNED);

    private static final Set<MinutesStatus> SIGNABLE = Set.of(
            MinutesStatus.DRAFT, MinutesStatus.CHANGES_REQUESTED);

    private final MinutesDraftRepository draftRepository;
    private final MinutesSectionRepository sectionRepository;
    private final SecretaryAccessChecker accessChecker;
    private final AuditService auditService;

    public MinutesService(MinutesDraftRepository draftRepository,
                          MinutesSectionRepository sectionRepository,
                          SecretaryAccessChecker accessChecker,
                          AuditService auditService) {
        this.draftRepository = draftRepository;
        this.sectionRepository = sectionRepository;
        this.accessChecker = accessChecker;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<MinutesDraftDto> list(UUID tontineId, UUID userId) {
        accessChecker.requireSecretary(userId, tontineId);
        return draftRepository.findByTontineIdOrderByCreatedAtDesc(tontineId).stream()
                .map(d -> MinutesDraftDto.from(d, sectionsOf(d.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public MinutesDraftDto findById(UUID id, UUID tontineId, UUID userId) {
        accessChecker.requireSecretary(userId, tontineId);
        MinutesDraft d = loadInTontine(id, tontineId);
        return MinutesDraftDto.from(d, sectionsOf(d.getId()));
    }

    @Transactional
    public MinutesDraftDto replaceSections(UUID id, UUID tontineId, UUID userId,
                                            UpdateMinutesSectionsRequest req) {
        accessChecker.requireSecretary(userId, tontineId);
        MinutesDraft d = loadInTontine(id, tontineId);
        if (d.getStatus() == MinutesStatus.PUBLISHED) {
            throw new ApiException("MINUTES_PUBLISHED",
                    "Le proces-verbal est deja publie", HttpStatus.CONFLICT);
        }
        if (!EDITABLE.contains(d.getStatus()) && d.getStatus() != MinutesStatus.PRESIDENT_SIGNED) {
            throw new ApiException("MINUTES_NOT_EDITABLE",
                    "Le proces-verbal n'est plus modifiable", HttpStatus.CONFLICT);
        }
        sectionRepository.deleteByMinutesDraftId(d.getId());
        sectionRepository.flush();
        int idx = 0;
        for (UpdateMinutesSectionsRequest.SectionInput s : req.sections()) {
            MinutesSectionEntity entity = new MinutesSectionEntity();
            entity.setMinutesDraftId(d.getId());
            entity.setSectionKey(s.key());
            entity.setTitle(s.title());
            entity.setContent(s.content());
            entity.setRequired(s.required());
            entity.setOrderIdx(idx++);
            sectionRepository.save(entity);
        }
        draftRepository.save(d);
        auditService.record(userId, "MINUTES_UPDATE_SECTIONS", "MinutesDraft", d.getId().toString(),
                tontineId, "{\"count\":" + req.sections().size() + "}");
        return MinutesDraftDto.from(d, sectionsOf(d.getId()));
    }

    @Transactional
    public MinutesDraftDto sign(UUID id, UUID tontineId, UUID userId) {
        accessChecker.requireSecretary(userId, tontineId);
        MinutesDraft d = loadInTontine(id, tontineId);
        if (!SIGNABLE.contains(d.getStatus())) {
            throw new ApiException("MINUTES_INVALID_STATE",
                    "Signature impossible dans l'etat actuel", HttpStatus.CONFLICT);
        }
        d.setStatus(MinutesStatus.SECRETARY_SIGNED);
        d.setSecretarySignedAt(Instant.now());
        MinutesDraft saved = draftRepository.save(d);
        auditService.record(userId, "MINUTES_SECRETARY_SIGN", "MinutesDraft", id.toString(),
                tontineId, null);
        return MinutesDraftDto.from(saved, sectionsOf(saved.getId()));
    }

    private MinutesDraft loadInTontine(UUID id, UUID tontineId) {
        MinutesDraft d = draftRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MinutesDraft", id));
        if (!d.getTontineId().equals(tontineId)) {
            throw new ApiException("FORBIDDEN", "Tontine non concordante", HttpStatus.FORBIDDEN);
        }
        return d;
    }

    private List<MinutesSectionDto> sectionsOf(UUID draftId) {
        return sectionRepository.findByMinutesDraftIdOrderByOrderIdxAsc(draftId).stream()
                .map(MinutesSectionDto::from)
                .toList();
    }
}
