package cm.ftg.tontine.integration.mobilemoney.gateway;

import cm.ftg.tontine.treasurer.mobilemoney.enums.MobileMoneyProvider;
import java.math.BigDecimal;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.mobile-money.provider", havingValue = "simulator", matchIfMissing = true)
public class SimulatedMobileMoneyGateway implements MobileMoneyGateway {

    private static final Logger log = LoggerFactory.getLogger(SimulatedMobileMoneyGateway.class);

    @Override
    public PaymentResult requestPayment(MobileMoneyProvider provider, String fromPhone,
                                         BigDecimal amount, String externalReference,
                                         String description) {
        String ref = "SIM-" + UUID.randomUUID();
        log.info("[MOMO-SIM] requestPayment provider={} ref={} ext={} amount=*** desc=\"{}\"",
                provider, ref, externalReference, description);
        return new PaymentResult(ref, PaymentStatus.PENDING, "Simulated request accepted");
    }

    @Override
    public PaymentResult disburse(MobileMoneyProvider provider, String toPhone,
                                   BigDecimal amount, String externalReference,
                                   String description) {
        String ref = "SIM-" + UUID.randomUUID();
        log.info("[MOMO-SIM] disburse provider={} ref={} ext={} amount=*** desc=\"{}\"",
                provider, ref, externalReference, description);
        return new PaymentResult(ref, PaymentStatus.SUCCESS, "Simulated disbursement");
    }

    @Override
    public PaymentStatus lookupStatus(MobileMoneyProvider provider, String gatewayReference) {
        return PaymentStatus.SUCCESS;
    }
}
