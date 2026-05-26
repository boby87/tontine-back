package cm.ftg.tontine.scheduling;

import cm.ftg.tontine.auth.repository.OtpCodeRepository;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "app.scheduling.otp-cleanup.enabled", havingValue = "true", matchIfMissing = true)
public class OtpCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(OtpCleanupJob.class);
    private static final Duration GRACE_PERIOD = Duration.ofHours(24);

    private final OtpCodeRepository repository;

    public OtpCleanupJob(OtpCodeRepository repository) {
        this.repository = repository;
    }

    @Scheduled(cron = "${app.scheduling.otp-cleanup.cron:0 15 * * * *}")
    @Transactional
    public void purgeExpired() {
        Instant cutoff = Instant.now().minus(GRACE_PERIOD);
        int deleted = repository.deleteByExpiresAtBefore(cutoff);
        if (deleted > 0) {
            log.info("[OtpCleanupJob] {} codes OTP supprimes (cutoff={})", deleted, cutoff);
        }
    }
}
