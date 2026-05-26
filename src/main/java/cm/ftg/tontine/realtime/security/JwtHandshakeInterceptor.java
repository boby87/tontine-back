package cm.ftg.tontine.realtime.security;

import cm.ftg.tontine.security.JwtService;
import java.net.URI;
import java.util.Map;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    public static final String ATTR_TOKEN_USER_ID = "jwt.userId";
    public static final String ATTR_TOKEN_EMAIL = "jwt.email";

    private final JwtService jwtService;

    public JwtHandshakeInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = extractToken(request.getURI());
        if (token != null) {
            try {
                JwtService.ParsedToken parsed = jwtService.parse(token);
                if (parsed.type() == JwtService.TokenType.ACCESS) {
                    attributes.put(ATTR_TOKEN_USER_ID, parsed.userId());
                }
            } catch (JwtService.InvalidJwtException ignored) {
                // L'auth definitive est faite sur la frame STOMP CONNECT
            }
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // no-op
    }

    private String extractToken(URI uri) {
        String query = uri.getQuery();
        if (query == null) {
            return null;
        }
        for (String part : query.split("&")) {
            int eq = part.indexOf('=');
            if (eq > 0) {
                String key = part.substring(0, eq);
                if ("access_token".equals(key) || "token".equals(key)) {
                    return part.substring(eq + 1);
                }
            }
        }
        return null;
    }
}
