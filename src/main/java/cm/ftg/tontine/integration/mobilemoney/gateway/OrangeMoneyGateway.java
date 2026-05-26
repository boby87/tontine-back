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
 * Scaffold Orange Money. A completer :
 *  - obtenir le bearer token via /oauth/v3/token (client_credentials)
 *  - initier la transaction via /webpayment/v1/transactioninit
 *  - decaissement via /b2c/v1/transfer
 *  - polling status sur /webpayment/v1/transactionstatus/{paymentToken}
 * Cf. https://developer.orange.com
 */
@Component
@ConditionalOnProperty(name = "app.mobile-money.provider", havingValue = "orange")
public class OrangeMoneyGateway implements MobileMoneyGateway {

    private static final Logger log = LoggerFactory.getLogger(OrangeMoneyGateway.class);

    private final MobileMoneyProperties properties;
    private final RestClient client;

    public OrangeMoneyGateway(MobileMoneyProperties properties) {
        this.properties = properties;
        MobileMoneyProperties.Orange cfg = properties.getOrange();
        this.client = RestClient.builder()
                .baseUrl(cfg.getBaseUrl())
                .build();
    }

    @Override
    public PaymentResult requestPayment(MobileMoneyProvider provider, String fromPhone,
                                         BigDecimal amount, String externalReference,
                                         String description) {
        String reference = UUID.randomUUID().toString();
        Map<String, Object> body = Map.of(
                "merchant_key", properties.getOrange().getMerchantKey(),
                "currency", "XAF",
                "order_id", externalReference,
                "amount", amount.toPlainString(),
                "notif_url", properties.getOrange().getCallbackUrl(),
                "reference", reference);
        try {
            client.post()
                    .uri("/orange-money-webpay/v1/webpayment")
                    .header("Authorization", "Bearer " + obtainToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            return new PaymentResult(reference, PaymentStatus.PENDING, "Accepted");
        } catch (RestClientException e) {
            log.warn("Orange requestPayment echec ref={} : {}", reference, e.getMessage());
            return new PaymentResult(reference, PaymentStatus.FAILED, e.getMessage());
        }
    }

    @Override
    public PaymentResult disburse(MobileMoneyProvider provider, String toPhone,
                                   BigDecimal amount, String externalReference,
                                   String description) {
        // TODO : implementer /b2c/v1/transfer (requiert habilitation B2C cote Orange)
        log.warn("Orange disburse non encore implemente, fallback FAILED");
        return new PaymentResult(UUID.randomUUID().toString(), PaymentStatus.FAILED,
                "Orange B2C disbursement non implemente");
    }

    @Override
    public PaymentStatus lookupStatus(MobileMoneyProvider provider, String gatewayReference) {
        // TODO : GET /orange-money-webpay/v1/transactionstatus/{paymentToken}
        return PaymentStatus.UNKNOWN;
    }

    private String obtainToken() {
        // TODO : cache + POST /oauth/v3/token client_credentials
        return "TODO";
    }
}
