package cm.ftg.tontine.scheduling;

import cm.ftg.tontine.audit.service.AuditService;
import cm.ftg.tontine.president.presidencytransfer.dto.PresidencyTransferDto;
import cm.ftg.tontine.president.presidencytransfer.entity.PresidencyTransfer;
import cm.ftg.tontine.president.presidencytransfer.enums.PresidencyTransferStatus;
import cm.ftg.tontine.president.presidencytransfer.repository.PresidencyTransferRepository;
import cm.ftg.tontine.realtime.RealtimeEventPublisher;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Passe les transferts de presidence PENDING a EXPIRED lorsque leur echeance est depassee.
 */
@Component
@ConditionalOnProperty(name = "app.scheduling.presidency-transfer-expiration.enabled", havingValue = "true", matchIfMissing = true)
public class PresidencyTransferExpirationJob {

    private static final Logger log = LoggerFactory.getLogger(PresidencyTransferExpirationJob.class);
    private static final String QUEUE = "/queue/presidency-transfer";

    private final PresidencyTransferRepository transferRepository;
    private final AuditService auditService;
    private final RealtimeEventPublisher realtime;

    public PresidencyTransferExpirationJob(PresidencyTransferRepository transferRepository,
                                           AuditService auditService,
                                           RealtimeEventPublisher realtime) {
        this.transferRepository = transferRepository;
        this.auditService = auditService;
        this.realtime = realtime;
    }

    @Scheduled(fixedDelayString = "${app.scheduling.presidency-transfer-expiration.fixed-delay-ms:300000}")
    @Transactional
    public void expirePendingTransfers() {
        Instant now = Instant.now();
        List<PresidencyTransfer> stale = transferRepository.findByStatusAndExpiresAtBefore(
                PresidencyTransferStatus.PENDING, now);
        if (stale.isEmpty()) {
            return;
        }
        for (PresidencyTransfer t : stale) {
            t.setStatus(PresidencyTransferStatus.EXPIRED);
            transferRepository.save(t);
            auditService.record(null, "PRESIDENCY_TRANSFER_EXPIRE", "PresidencyTransfer",
                    t.getId().toString(), t.getTontineId(), null);
            realtime.toUser(t.getInitiatedByUserId(), QUEUE, "presidency-transfer.expired",
                    PresidencyTransferDto.from(t));
        }
        log.info("[PresidencyTransferExpirationJob] {} transferts passes en EXPIRED", stale.size());
    }
}
