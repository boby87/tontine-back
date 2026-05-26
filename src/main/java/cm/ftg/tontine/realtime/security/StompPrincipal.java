package cm.ftg.tontine.realtime.security;

import java.security.Principal;
import java.util.UUID;

public record StompPrincipal(UUID userId, String email) implements Principal {

    @Override
    public String getName() {
        return userId.toString();
    }
}
