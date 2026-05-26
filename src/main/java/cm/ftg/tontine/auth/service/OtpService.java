package cm.ftg.tontine.auth.service;

import cm.ftg.tontine.auth.entity.OtpCode;
import cm.ftg.tontine.auth.repository.OtpCodeRepository;
import cm.ftg.tontine.common.enums.OtpPurpose;
import cm.ftg.tontine.common.exception.ApiException;
import java.security.SecureRandom;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OtpService {

    private static final Logger log = LoggerFactory.getLogger(OtpService.class);
    private static final int MAX_ATTEMPTS = 5;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final OtpCodeRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final long ttlSeconds;

    public OtpService(OtpCodeRepository repository,
                      PasswordEncoder passwordEncoder,
                      @Value("${app.security.otp.ttl-seconds:300}") long ttlSeconds) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.ttlSeconds = ttlSeconds;
    }

    @Transactional
    public void issue(String identifier, OtpPurpose purpose) {
        String code = generateCode();
        OtpCode otp = new OtpCode();
        otp.setIdentifier(identifier);
        otp.setCodeHash(passwordEncoder.encode(code));
        otp.setPurpose(purpose);
        otp.setExpiresAt(Instant.now().plusSeconds(ttlSeconds));
        repository.save(otp);
        // En production : envoyer via SMS / Email. En dev : on logue avec un masque.
        log.info("[OTP] purpose={} identifier={} code={} (a remplacer par envoi reel)",
                purpose, maskIdentifier(identifier), code);
    }

    @Transactional
    public void verifyAndConsume(String identifier, String code, OtpPurpose purpose) {
        OtpCode otp = repository
                .findTopByIdentifierAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(identifier, purpose)
                .orElseThrow(() -> new ApiException("AUTH_OTP_INVALID",
                        "OTP introuvable", HttpStatus.UNAUTHORIZED));
        if (otp.isExpired()) {
            throw new ApiException("AUTH_OTP_INVALID", "OTP expire", HttpStatus.UNAUTHORIZED);
        }
        if (otp.getAttempts() >= MAX_ATTEMPTS) {
            throw new ApiException("AUTH_OTP_INVALID",
                    "Trop de tentatives, demandez un nouveau code", HttpStatus.UNAUTHORIZED);
        }
        otp.setAttempts(otp.getAttempts() + 1);
        if (!passwordEncoder.matches(code, otp.getCodeHash())) {
            repository.save(otp);
            throw new ApiException("AUTH_OTP_INVALID", "Code incorrect", HttpStatus.UNAUTHORIZED);
        }
        otp.setConsumedAt(Instant.now());
        repository.save(otp);
    }

    private String generateCode() {
        int n = RANDOM.nextInt(1_000_000);
        return String.format("%06d", n);
    }

    private String maskIdentifier(String identifier) {
        if (identifier == null || identifier.length() < 4) {
            return "***";
        }
        return identifier.substring(0, Math.min(3, identifier.length())) + "***";
    }
}
