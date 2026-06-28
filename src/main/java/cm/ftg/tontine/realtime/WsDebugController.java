package cm.ftg.tontine.realtime;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint de diagnostic pour inspecter les sessions WebSocket actives.
 * Appeler GET /api/debug/ws-users pendant le test pour verifier
 * que le mobile est bien enregistre dans SimpUserRegistry.
 * A supprimer avant la mise en production.
 */
@RestController
@RequestMapping("/debug/ws")
public class WsDebugController {

    private final SimpUserRegistry userRegistry;

    public WsDebugController(SimpUserRegistry userRegistry) {
        this.userRegistry = userRegistry;
    }

    @GetMapping("/users")
    public Map<String, Object> connectedUsers() {
        List<Map<String, Object>> users = userRegistry.getUsers().stream()
                .map(user -> Map.<String, Object>of(
                        "name", user.getName(),
                        "sessions", user.getSessions().stream()
                                .map(s -> Map.of("id", s.getId(), "subscriptions",
                                        s.getSubscriptions().stream()
                                                .map(sub -> Map.of("id", sub.getId(), "destination", sub.getDestination()))
                                                .toList()))
                                .toList()))
                .collect(Collectors.toList());
        return Map.of(
                "count", userRegistry.getUserCount(),
                "users", users);
    }
}
