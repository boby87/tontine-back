package cm.ftg.tontine.integration.mobilemoney.gateway;

import cm.ftg.tontine.integration.mobilemoney.config.MobileMoneyProperties;
import cm.ftg.tontine.treasurer.mobilemoney.enums.MobileMoneyProvider;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Scaffold MTN MoMo API. Implementation a finaliser :
 *  - obtenir/cacher le bearer token via /collection/token et /disbursement/token
 *  - implementer requestPayment via POST /collection/v1_0/requesttopay
 *  - implementer disburse via POST /disbursement/v1_0/transfer
 *  - lookupStatus via GET /{collection|disbursement}/v1_0/.../referenceId/{ref}
 * Cf. https://momodeveloper.mtn.com/
 */
@Component
@ConditionalOnProperty(name = "app.mobile-money.provider", havingValue = "mtn")
public class MtnMomoGateway implements MobileMoneyGateway {

    private static final Logger log = LoggerFactory.getLogger(MtnMomoGateway.class);

    private final MobileMoneyProperties properties;
    private final RestClient client;

    public MtnMomoGateway(MobileMoneyProperties properties) {
        this.properties = properties;
        MobileMoneyProperties.Mtn cfg = properties.getMtn();
        this.client = RestClient.builder()
                .baseUrl(cfg.getBaseUrl())
                .defaultHeader("Ocp-Apim-Subscription-Key", cfg.getSubscriptionKey())
                .defaultHeader("X-Target-Environment", cfg.getTargetEnvironment())
                .build();
    }

    @Override
    public PaymentResult requestPayment(MobileMoneyProvider provider, String fromPhone,
                                         BigDecimal amount, String externalReference,
                                         String description) {
        String reference = UUID.randomUUID().toString();
        Map<String, Object> body = Map.of(
                "amount", amount.toPlainString(),
                "currency", "XAF",
                "externalId", externalReference,
                "payer", Map.of("partyIdType", "MSISDN", "partyId", normalize(fromPhone)),
                "payerMessage", description == null ? "" : description,
                "payeeNote", description == null ? "" : description);
        try {
            client.post()
                    .uri("/collection/v1_0/requesttopay")
                    .header("X-Reference-Id", reference)
                    .header("Authorization", "Bearer " + obtainCollectionToken())
                    .header("X-Callback-Url", properties.getMtn().getCallbackUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            return new PaymentResult(reference, PaymentStatus.PENDING, "Accepted");
        } catch (RestClientException e) {
            log.warn("MTN requestPayment echec ref={} : {}", reference, e.getMessage());
            return new PaymentResult(reference, PaymentStatus.FAILED, e.getMessage());
        }
    }

    @Override
    public PaymentResult disburse(MobileMoneyProvider provider, String toPhone,
                                   BigDecimal amount, String externalReference,
                                   String description) {
        String reference = UUID.randomUUID().toString();
        Map<String, Object> body = Map.of(
                "amount", amount.toPlainString(),
                "currency", "XAF",
                "externalId", externalReference,
                "payee", Map.of("partyIdType", "MSISDN", "partyId", normalize(toPhone)),
                "payerMessage", description == null ? "" : description,
                "payeeNote", description == null ? "" : description);
        try {
            client.post()
                    .uri("/disbursement/v1_0/transfer")
                    .header("X-Reference-Id", reference)
                    .header("Authorization", "Bearer " + obtainDisbursementToken())
                    .header("X-Callback-Url", properties.getMtn().getCallbackUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            return new PaymentResult(reference, PaymentStatus.PENDING, "Accepted");
        } catch (RestClientException e) {
            log.warn("MTN disburse echec ref={} : {}", reference, e.getMessage());
            return new PaymentResult(reference, PaymentStatus.FAILED, e.getMessage());
        }
    }

    @Override
    public PaymentStatus lookupStatus(MobileMoneyProvider provider, String gatewayReference) {
        // TODO : appeler /{collection|disbursement}/v1_0/.../referenceId/{ref}
        return PaymentStatus.UNKNOWN;
    }

    private String obtainCollectionToken() {
        // TODO : cache + POST /collection/token avec Basic Auth (apiUser:apiKey)
        return "TODO";
    }

    private String obtainDisbursementToken() {
        // TODO : cache + POST /disbursement/token
        return "TODO";
    }

    private String normalize(String phone) {
        return phone == null ? "" : phone.replaceAll("[^0-9]", "");
    }
}
