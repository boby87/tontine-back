package cm.ftg.tontine.integration.mobilemoney.webhook;

import cm.ftg.tontine.common.exception.ApiException;
import cm.ftg.tontine.integration.mobilemoney.config.MobileMoneyProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class WebhookSignatureVerifier {

    private final MobileMoneyProperties properties;

    public WebhookSignatureVerifier(MobileMoneyProperties properties) {
        this.properties = properties;
    }

    public void verify(String rawBody, String providedSignature) {
        String secret = properties.getWebhookSecret();
        if (secret == null || secret.isBlank()) {
            // Sans secret configure, on accepte (mode dev/simulator). En prod, refuser.
            return;
        }
        if (providedSignature == null || providedSignature.isBlank()) {
            throw new ApiException("WEBHOOK_SIGNATURE_MISSING",
                    "Signature absente", HttpStatus.UNAUTHORIZED);
        }
        String expected = hmacSha256Hex(rawBody, secret);
        byte[] a = expected.getBytes(StandardCharsets.UTF_8);
        byte[] b = providedSignature.getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(a, b)) {
            throw new ApiException("WEBHOOK_SIGNATURE_INVALID",
                    "Signature invalide", HttpStatus.UNAUTHORIZED);
        }
    }

    private String hmacSha256Hex(String body, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(raw.length * 2);
            for (byte x : raw) {
                hex.append(String.format("%02x", x));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("HMAC indisponible", e);
        }
    }
}
