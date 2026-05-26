package cm.ftg.tontine.realtime;

import cm.ftg.tontine.realtime.event.RealtimeEvent;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class RealtimeEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(RealtimeEventPublisher.class);

    private final SimpMessagingTemplate messagingTemplate;

    public RealtimeEventPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void toUser(UUID userId, String queue, String eventType, Object payload) {
        if (userId == null) {
            return;
        }
        safeSendToUser(userId.toString(), queue, RealtimeEvent.of(eventType, payload));
    }

    public void toSession(UUID tontineId, UUID sessionId, String eventType, Object payload) {
        String topic = "/topic/tontines/" + tontineId + "/sessions/" + sessionId;
        safeSend(topic, RealtimeEvent.of(eventType, payload));
    }

    public void toPresidentDashboard(UUID tontineId, String eventType, Object payload) {
        safeSend("/topic/tontines/" + tontineId + "/dashboards/president",
                RealtimeEvent.of(eventType, payload));
    }

    public void toTreasurerDashboard(UUID tontineId, String eventType, Object payload) {
        safeSend("/topic/tontines/" + tontineId + "/dashboards/treasurer",
                RealtimeEvent.of(eventType, payload));
    }

    public void toCensorDashboard(UUID tontineId, String eventType, Object payload) {
        safeSend("/topic/tontines/" + tontineId + "/dashboards/censor",
                RealtimeEvent.of(eventType, payload));
    }

    public void toVote(UUID tontineId, UUID voteId, String eventType, Object payload) {
        safeSend("/topic/tontines/" + tontineId + "/votes/" + voteId,
                RealtimeEvent.of(eventType, payload));
    }

    public void toTontine(UUID tontineId, String eventType, Object payload) {
        safeSend("/topic/tontines/" + tontineId, RealtimeEvent.of(eventType, payload));
    }

    private void safeSend(String destination, RealtimeEvent event) {
        try {
            messagingTemplate.convertAndSend(destination, event);
        } catch (MessagingException ex) {
            log.warn("WebSocket broadcast failed ({}): {}", destination, ex.getMessage());
        }
    }

    private void safeSendToUser(String userId, String destination, RealtimeEvent event) {
        try {
            messagingTemplate.convertAndSendToUser(userId, destination, event);
        } catch (MessagingException ex) {
            log.warn("WebSocket user-broadcast failed (user={}, dest={}): {}", userId, destination, ex.getMessage());
        }
    }
}
