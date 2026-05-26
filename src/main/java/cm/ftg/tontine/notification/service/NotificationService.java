package cm.ftg.tontine.notification.service;

import cm.ftg.tontine.common.exception.ApiException;
import cm.ftg.tontine.common.exception.ResourceNotFoundException;
import cm.ftg.tontine.notification.dto.NotificationDto;
import cm.ftg.tontine.notification.entity.AppNotification;
import cm.ftg.tontine.notification.enums.NotificationCategory;
import cm.ftg.tontine.notification.enums.NotificationKind;
import cm.ftg.tontine.notification.repository.AppNotificationRepository;
import cm.ftg.tontine.realtime.RealtimeEventPublisher;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final AppNotificationRepository repository;
    private final RealtimeEventPublisher realtime;

    public NotificationService(AppNotificationRepository repository, RealtimeEventPublisher realtime) {
        this.repository = repository;
        this.realtime = realtime;
    }

    @Transactional
    public AppNotification publish(UUID userId, UUID tontineId, NotificationKind kind,
                                    NotificationCategory category, String title,
                                    String message, String link) {
        if (userId == null) {
            log.warn("Notification skipped: userId is null (category={}, title={})", category, title);
            return null;
        }
        AppNotification n = new AppNotification();
        n.setUserId(userId);
        n.setTontineId(tontineId);
        n.setKind(kind != null ? kind : NotificationKind.INFO);
        n.setCategory(category != null ? category : NotificationCategory.GENERAL);
        n.setTitle(truncate(title, 200));
        n.setMessage(truncate(message, 2000));
        n.setLink(link);
        AppNotification saved = repository.save(n);
        realtime.toUser(userId, "/queue/notifications", "notification.created",
                NotificationDto.from(saved));
        return saved;
    }

    @Transactional(readOnly = true)
    public Page<AppNotification> list(UUID userId, UUID tontineId, Boolean unreadOnly, Pageable pageable) {
        if (Boolean.TRUE.equals(unreadOnly)) {
            return repository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId, pageable);
        }
        if (tontineId != null) {
            return repository.findByUserIdAndTontineIdOrderByCreatedAtDesc(userId, tontineId, pageable);
        }
        return repository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    @Transactional(readOnly = true)
    public long countUnread(UUID userId) {
        return repository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public AppNotification markRead(UUID notificationId, UUID userId) {
        AppNotification n = loadOwned(notificationId, userId);
        if (!n.isRead()) {
            n.setRead(true);
            n.setReadAt(Instant.now());
        }
        return repository.save(n);
    }

    @Transactional
    public int markAllRead(UUID userId) {
        return repository.markAllReadForUser(userId, Instant.now());
    }

    @Transactional
    public void delete(UUID notificationId, UUID userId) {
        AppNotification n = loadOwned(notificationId, userId);
        repository.delete(n);
    }

    private AppNotification loadOwned(UUID id, UUID userId) {
        AppNotification n = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", id));
        if (!n.getUserId().equals(userId)) {
            throw new ApiException("FORBIDDEN",
                    "Cette notification ne vous appartient pas", HttpStatus.FORBIDDEN);
        }
        return n;
    }

    private String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() > max ? s.substring(0, max) : s;
    }
}
