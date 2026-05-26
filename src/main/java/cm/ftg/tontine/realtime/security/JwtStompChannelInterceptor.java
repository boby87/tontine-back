package cm.ftg.tontine.realtime.security;

import cm.ftg.tontine.auth.entity.UserEntity;
import cm.ftg.tontine.auth.repository.UserRepository;
import cm.ftg.tontine.security.JwtService;
import java.util.UUID;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;

public class JwtStompChannelInterceptor implements ChannelInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtStompChannelInterceptor(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() != StompCommand.CONNECT) {
            return message;
        }

        UUID userId = extractFromAuthorizationHeader(accessor);
        if (userId == null) {
            Object attr = accessor.getSessionAttributes() != null
                    ? accessor.getSessionAttributes().get(JwtHandshakeInterceptor.ATTR_TOKEN_USER_ID)
                    : null;
            if (attr instanceof UUID resolved) {
                userId = resolved;
            }
        }

        if (userId == null) {
            throw new IllegalArgumentException("Authentification requise pour ce canal WebSocket");
        }

        UserEntity user = userRepository.findById(userId)
                .filter(UserEntity::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur invalide ou desactive"));
        StompPrincipal principal = new StompPrincipal(user.getId(), user.getEmail());
        accessor.setUser(principal);
        accessor.setLeaveMutable(true);
        return message;
    }

    private UUID extractFromAuthorizationHeader(StompHeaderAccessor accessor) {
        String header = accessor.getFirstNativeHeader("Authorization");
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return null;
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();
        try {
            JwtService.ParsedToken parsed = jwtService.parse(token);
            if (parsed.type() != JwtService.TokenType.ACCESS) {
                return null;
            }
            return parsed.userId();
        } catch (JwtService.InvalidJwtException ex) {
            return null;
        }
    }
}
