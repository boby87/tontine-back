package cm.ftg.tontine.auth.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

/**
 * Service TOTP (RFC 6238) pour la 2FA via Google Authenticator / Authy.
 * Le secret est stocke en Base32 (format natif des apps TOTP).
 */
@Service
public class TotpService {

    private static final String BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final int TOTP_PERIOD = 30;
    private static final int TOTP_DIGITS = 6;
    private static final int WINDOW = 1;
    private static final int QR_SIZE = 200;

    /** Genere un nouveau secret TOTP aleatoire encode en Base32. */
    public String generateSecret() {
        byte[] bytes = new byte[20];
        new SecureRandom().nextBytes(bytes);
        return base32Encode(bytes);
    }

    /** Construit l'URI otpauth:// pour l'enregistrement dans l'app TOTP. */
    public String buildOtpauthUri(String email, String issuer, String secret) {
        String encodedIssuer = URLEncoder.encode(issuer, StandardCharsets.UTF_8);
        String encodedAccount = URLEncoder.encode(issuer + ":" + email, StandardCharsets.UTF_8);
        return "otpauth://totp/" + encodedAccount
                + "?secret=" + secret
                + "&issuer=" + encodedIssuer
                + "&algorithm=SHA1&digits=" + TOTP_DIGITS + "&period=" + TOTP_PERIOD;
    }

    /** Genere l'image QR code PNG a partir d'une URI otpauth://. */
    public byte[] generateQrCodePng(String otpauthUri) {
        try {
            BitMatrix matrix = new MultiFormatWriter()
                    .encode(otpauthUri, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Impossible de generer le QR code TOTP", e);
        }
    }

    /**
     * Verifie un code TOTP a 6 chiffres.
     * Accepte une fenetre de +-1 periode (30 s) pour la derive d'horloge.
     */
    public boolean verify(String base32Secret, String code) {
        if (code == null || code.length() != TOTP_DIGITS) {
            return false;
        }
        long currentStep = System.currentTimeMillis() / 1000 / TOTP_PERIOD;
        for (long step = currentStep - WINDOW; step <= currentStep + WINDOW; step++) {
            if (computeHotp(base32Secret, step).equals(code)) {
                return true;
            }
        }
        return false;
    }

    // ── Implémentation HOTP (RFC 4226) ───────────────────────────────────────

    private String computeHotp(String base32Secret, long counter) {
        try {
            byte[] key = base32Decode(base32Secret);
            byte[] msg = ByteBuffer.allocate(8).putLong(counter).array();
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "RAW"));
            byte[] hash = mac.doFinal(msg);
            int offset = hash[hash.length - 1] & 0x0f;
            int truncated = ((hash[offset] & 0x7f) << 24)
                    | ((hash[offset + 1] & 0xff) << 16)
                    | ((hash[offset + 2] & 0xff) << 8)
                    | (hash[offset + 3] & 0xff);
            return String.format("%0" + TOTP_DIGITS + "d", truncated % 1_000_000);
        } catch (Exception e) {
            throw new IllegalStateException("Erreur calcul HOTP", e);
        }
    }

    // ── Base32 (RFC 4648) ────────────────────────────────────────────────────

    private static String base32Encode(byte[] data) {
        StringBuilder sb = new StringBuilder();
        int buffer = 0, bitsLeft = 0;
        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xff);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                bitsLeft -= 5;
                sb.append(BASE32_ALPHABET.charAt((buffer >> bitsLeft) & 0x1f));
            }
        }
        if (bitsLeft > 0) {
            sb.append(BASE32_ALPHABET.charAt((buffer << (5 - bitsLeft)) & 0x1f));
        }
        return sb.toString();
    }

    private static byte[] base32Decode(String encoded) {
        String upper = encoded.toUpperCase().replaceAll("=", "");
        int outLen = upper.length() * 5 / 8;
        byte[] out = new byte[outLen];
        int buffer = 0, bitsLeft = 0, idx = 0;
        for (char c : upper.toCharArray()) {
            int val = BASE32_ALPHABET.indexOf(c);
            if (val < 0) continue;
            buffer = (buffer << 5) | val;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                bitsLeft -= 8;
                out[idx++] = (byte) ((buffer >> bitsLeft) & 0xff);
            }
        }
        return out;
    }
}
