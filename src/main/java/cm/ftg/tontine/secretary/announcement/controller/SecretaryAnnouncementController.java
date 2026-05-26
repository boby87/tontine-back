package cm.ftg.tontine.secretary.announcement.controller;

import cm.ftg.tontine.audit.service.AuditService;
import cm.ftg.tontine.auth.entity.UserEntity;
import cm.ftg.tontine.auth.repository.UserRepository;
import cm.ftg.tontine.common.dto.ApiResponse;
import cm.ftg.tontine.common.enums.NotificationChannel;
import cm.ftg.tontine.common.exception.ApiException;
import cm.ftg.tontine.president.announcement.dto.AnnouncementDto;
import cm.ftg.tontine.president.announcement.dto.CreateAnnouncementRequest;
import cm.ftg.tontine.president.announcement.entity.Announcement;
import cm.ftg.tontine.president.announcement.repository.AnnouncementRepository;
import cm.ftg.tontine.president.common.TontineIdResolver;
import cm.ftg.tontine.secretary.security.SecretaryAccessChecker;
import cm.ftg.tontine.security.AuthenticatedUser;
import jakarta.validation.Valid;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/secretary/announcements")
public class SecretaryAnnouncementController {

    private static final Set<NotificationChannel> ALLOWED_CHANNELS =
            EnumSet.of(NotificationChannel.IN_APP, NotificationChannel.SMS, NotificationChannel.EMAIL);

    private final AnnouncementRepository repository;
    private final SecretaryAccessChecker accessChecker;
    private final TontineIdResolver tontineIdResolver;
    private final AuditService auditService;
    private final UserRepository userRepository;

    public SecretaryAnnouncementController(AnnouncementRepository repository,
                                           SecretaryAccessChecker accessChecker,
                                           TontineIdResolver tontineIdResolver,
                                           AuditService auditService,
                                           UserRepository userRepository) {
        this.repository = repository;
        this.accessChecker = accessChecker;
        this.tontineIdResolver = tontineIdResolver;
        this.auditService = auditService;
        this.userRepository = userRepository;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ApiResponse<List<AnnouncementDto>> list(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        accessChecker.requireSecretary(user.id(), t);
        return ApiResponse.ok(repository.findByTontineIdOrderByPublishedAtDesc(t).stream()
                .map(AnnouncementDto::from)
                .toList());
    }

    @PostMapping
    @Transactional
    public ApiResponse<AnnouncementDto> publish(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId,
            @Valid @RequestBody CreateAnnouncementRequest req) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        accessChecker.requireSecretary(user.id(), t);
        Set<NotificationChannel> invalid = EnumSet.copyOf(req.channels());
        invalid.removeAll(ALLOWED_CHANNELS);
        if (!invalid.isEmpty()) {
            throw new ApiException("ANNOUNCEMENT_INVALID_CHANNEL",
                    "Canaux autorises: IN_APP, SMS, EMAIL", HttpStatus.valueOf(422));
        }
        UserEntity author = userRepository.findById(user.id())
                .orElseThrow(() -> new ApiException("USER_NOT_FOUND",
                        "Utilisateur introuvable", HttpStatus.NOT_FOUND));
        Announcement a = new Announcement();
        a.setTontineId(t);
        a.setAuthorUserId(user.id());
        a.setAuthorFullName(buildFullName(author));
        a.setTitle(req.title());
        a.setBody(req.body());
        a.setAudience(req.audience());
        a.setChannels(EnumSet.copyOf(req.channels()));
        Announcement saved = repository.save(a);
        auditService.record(user.id(), "ANNOUNCEMENT_PUBLISH", "Announcement",
                saved.getId().toString(), t, "{\"audience\":\"" + req.audience().name() + "\"}");
        return ApiResponse.ok(AnnouncementDto.from(saved), "Annonce publiee");
    }

    private String buildFullName(UserEntity u) {
        String first = u.getFirstName() == null ? "" : u.getFirstName();
        String last = u.getLastName() == null ? "" : u.getLastName();
        String full = (first + " " + last).trim();
        return full.isEmpty() ? u.getEmail() : full;
    }
}
