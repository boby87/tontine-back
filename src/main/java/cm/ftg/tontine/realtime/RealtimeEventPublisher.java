package cm.ftg.tontine.realtime;

import cm.ftg.tontine.realtime.event.RealtimeEvent;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Component;

@Component
public class RealtimeEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(RealtimeEventPublisher.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final SimpUserRegistry userRegistry;

    public RealtimeEventPublisher(SimpMessagingTemplate messagingTemplate,
                                  SimpUserRegistry userRegistry) {
        this.messagingTemplate = messagingTemplate;
        this.userRegistry = userRegistry;
    }

    /**
     * Envoie un evenement a toutes les sessions actives d'un utilisateur.
     *
     * Bypasse convertAndSendToUser : ce dernier envoie au brokerChannel sous
     * /user/{userId}/queue/... alors que les abonnements sont enregistres sous
     * /queue/{name}-user{sessionId} (apres traduction par UserDestinationMessageHandler).
     * Cette traduction fonctionne pour SockJS (web) mais echoue silencieusement pour
     * les sessions WebSocket natives (mobile Ionic/Capacitor).
     * On construit la destination session-specifique directement pour garantir la
     * livraison sur les deux transports.
     */
    public void toUser(UUID userId, String queue, String eventType, Object payload) {
        if (userId == null) {
            return;
        }
        SimpUser user = userRegistry.getUser(userId.toString());
        if (user == null || user.getSessions().isEmpty()) {
            log.info("[WS] toUser: userId={} non connecte (aucune session active) - message ignore (type={})",
                    userId, eventType);
            return;
        }
        RealtimeEvent event = RealtimeEvent.of(eventType, payload);
        log.info("[WS] toUser: userId={} - {} session(s) trouvee(s), envoi type={}",
                userId, user.getSessions().size(), eventType);
        user.getSessions().forEach(session -> {
            String dest = queue + "-user" + session.getId();
            log.info("[WS] toUser: -> destination={}", dest);
            safeSend(dest, event);
        });
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

}
