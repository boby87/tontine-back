package cm.ftg.tontine.president.announcement.service;

import cm.ftg.tontine.audit.service.AuditService;
import cm.ftg.tontine.auth.entity.UserEntity;
import cm.ftg.tontine.auth.repository.UserRepository;
import cm.ftg.tontine.common.enums.NotificationChannel;
import cm.ftg.tontine.common.exception.ApiException;
import cm.ftg.tontine.president.announcement.dto.AnnouncementDto;
import cm.ftg.tontine.president.announcement.dto.CreateAnnouncementRequest;
import cm.ftg.tontine.president.announcement.entity.Announcement;
import cm.ftg.tontine.president.announcement.repository.AnnouncementRepository;
import cm.ftg.tontine.president.security.PresidentAccessChecker;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnnouncementService {

    private static final Set<NotificationChannel> ALLOWED_CHANNELS =
            EnumSet.of(NotificationChannel.IN_APP, NotificationChannel.SMS, NotificationChannel.EMAIL);

    private final AnnouncementRepository repository;
    private final PresidentAccessChecker accessChecker;
    private final AuditService auditService;
    private final UserRepository userRepository;

    public AnnouncementService(AnnouncementRepository repository,
                               PresidentAccessChecker accessChecker,
                               AuditService auditService,
                               UserRepository userRepository) {
        this.repository = repository;
        this.accessChecker = accessChecker;
        this.auditService = auditService;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<AnnouncementDto> list(UUID tontineId, UUID userId) {
        accessChecker.requirePresident(userId, tontineId);
        return repository.findByTontineIdOrderByPublishedAtDesc(tontineId).stream()
                .map(AnnouncementDto::from)
                .toList();
    }

    @Transactional
    public AnnouncementDto publish(UUID tontineId, UUID userId, CreateAnnouncementRequest req) {
        accessChecker.requirePresident(userId, tontineId);
        Set<NotificationChannel> invalid = EnumSet.copyOf(req.channels());
        invalid.removeAll(ALLOWED_CHANNELS);
        if (!invalid.isEmpty()) {
            throw new ApiException("ANNOUNCEMENT_INVALID_CHANNEL",
                    "Canaux autorises: IN_APP, SMS, EMAIL", HttpStatus.valueOf(422));
        }
        UserEntity author = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("USER_NOT_FOUND",
                        "Utilisateur introuvable", HttpStatus.NOT_FOUND));
        Announcement a = new Announcement();
        a.setTontineId(tontineId);
        a.setAuthorUserId(userId);
        a.setAuthorFullName(buildFullName(author));
        a.setTitle(req.title());
        a.setBody(req.body());
        a.setAudience(req.audience());
        a.setChannels(EnumSet.copyOf(req.channels()));
        Announcement saved = repository.save(a);
        auditService.record(userId, "ANNOUNCEMENT_PUBLISH", "Announcement", saved.getId().toString(),
                tontineId, "{\"audience\":\"" + req.audience().name() + "\"}");
        return AnnouncementDto.from(saved);
    }

    private String buildFullName(UserEntity u) {
        String first = u.getFirstName() == null ? "" : u.getFirstName();
        String last = u.getLastName() == null ? "" : u.getLastName();
        String full = (first + " " + last).trim();
        return full.isEmpty() ? u.getEmail() : full;
    }
}
