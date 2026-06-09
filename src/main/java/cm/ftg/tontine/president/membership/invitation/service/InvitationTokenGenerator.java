package cm.ftg.tontine.president.membership.invitation.service;

import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.stereotype.Component;

/**
 * Genere des tokens d'invitation : 24 octets SecureRandom encodes en
 * Base64 URL-safe sans padding (~32 caracteres). C'est un secret one-shot
 * court-vivant, stocke en clair (ne jamais le logger).
 */
@Component
public class InvitationTokenGenerator {

    private static final int TOKEN_BYTES = 24;

    private final SecureRandom secureRandom = new SecureRandom();
    private final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();

    public String generate() {
        byte[] buffer = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(buffer);
        return encoder.encodeToString(buffer);
    }
}
