package cm.ftg.tontine.notification.service;

import cm.ftg.tontine.notification.repository.AppNotificationRepository;
import cm.ftg.tontine.realtime.RealtimeEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class NotificationEventListener {

    private final RealtimeEventPublisher realtime;
    private final AppNotificationRepository repository;

    public NotificationEventListener(RealtimeEventPublisher realtime,
                                     AppNotificationRepository repository) {
        this.realtime = realtime;
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onNotificationReady(NotificationService.NotificationReadyEvent event) {
        realtime.toUser(event.userId(), "/queue/notifications", "notification.created", event.dto());
        long unread = repository.countByUserIdAndReadFalse(event.userId());
        realtime.toUser(event.userId(), "/queue/notifications/count", "notification.count.updated", unread);
    }
}
