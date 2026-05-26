package cm.ftg.tontine.integration.mobilemoney.webhook;

import cm.ftg.tontine.audit.service.AuditService;
import cm.ftg.tontine.common.dto.ApiResponse;
import cm.ftg.tontine.realtime.RealtimeEventPublisher;
import cm.ftg.tontine.treasurer.cashbox.enums.MovementDirection;
import cm.ftg.tontine.treasurer.mobilemoney.dto.MobileMoneyTransactionDto;
import cm.ftg.tontine.treasurer.mobilemoney.entity.MobileMoneyTransaction;
import cm.ftg.tontine.treasurer.mobilemoney.enums.MobileMoneyStatus;
import cm.ftg.tontine.treasurer.mobilemoney.repository.MobileMoneyTransactionRepository;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhooks/mobile-money")
public class MobileMoneyWebhookController {

    private static final Logger log = LoggerFactory.getLogger(MobileMoneyWebhookController.class);

    private final MobileMoneyTransactionRepository repository;
    private final WebhookSignatureVerifier signatureVerifier;
    private final AuditService auditService;
    private final RealtimeEventPublisher realtime;

    public MobileMoneyWebhookController(MobileMoneyTransactionRepository repository,
                                        WebhookSignatureVerifier signatureVerifier,
                                        AuditService auditService,
                                        RealtimeEventPublisher realtime) {
        this.repository = repository;
        this.signatureVerifier = signatureVerifier;
        this.auditService = auditService;
        this.realtime = realtime;
    }

    @PostMapping(value = "/{provider}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ApiResponse<MobileMoneyTransactionDto> ingest(
            @PathVariable String provider,
            @RequestHeader(value = "X-Webhook-Signature", required = false) String signature,
            @Valid @RequestBody MobileMoneyWebhookPayload payload) {
        // En reception, on materialise dans `ingestRaw`. Ici on revient sur un payload deja parse :
        // pour la verification HMAC sur le corps brut, voir si besoin d'un filtre dedie.
        signatureVerifier.verify(payload.toString(), signature);

        MobileMoneyTransaction tx = new MobileMoneyTransaction();
        tx.setTontineId(payload.tontineId());
        tx.setProvider(payload.provider());
        tx.setDirection(MovementDirection.IN);
        tx.setAmount(payload.amount());
        tx.setFromPhone(payload.fromPhone());
        tx.setExternalReference(payload.externalReference());
        tx.setStatus(MobileMoneyStatus.PENDING_APPROVAL);
        MobileMoneyTransaction saved = repository.save(tx);

        log.info("[MOMO-WEBHOOK] provider={} ext={} tontine={} status=PENDING_APPROVAL",
                provider, payload.externalReference(), payload.tontineId());
        auditService.record(null, "MOBILE_MONEY_WEBHOOK_IN", "MobileMoneyTransaction",
                saved.getId().toString(), saved.getTontineId(),
                "{\"provider\":\"" + payload.provider().name() + "\"}");
        realtime.toTreasurerDashboard(saved.getTontineId(), "mobile_money.pending",
                MobileMoneyTransactionDto.from(saved));
        return ApiResponse.ok(MobileMoneyTransactionDto.from(saved));
    }

    /** Helper utilise par un filtre HMAC si besoin de signer le corps brut. */
    public static byte[] asBytes(String s) {
        return s == null ? new byte[0] : s.getBytes(StandardCharsets.UTF_8);
    }
}
