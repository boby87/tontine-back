package cm.ftg.tontine.integration.mobilemoney.gateway;

import cm.ftg.tontine.treasurer.mobilemoney.enums.MobileMoneyProvider;
import java.math.BigDecimal;

public interface MobileMoneyGateway {

    /**
     * Initie une demande de paiement entrant (collect) : le client est notifie pour
     * approuver le debit de son compte mobile money.
     */
    PaymentResult requestPayment(MobileMoneyProvider provider,
                                  String fromPhone,
                                  BigDecimal amount,
                                  String externalReference,
                                  String description);

    /**
     * Decaisse vers un numero mobile money beneficiaire (disbursement).
     */
    PaymentResult disburse(MobileMoneyProvider provider,
                            String toPhone,
                            BigDecimal amount,
                            String externalReference,
                            String description);

    /**
     * Recupere le statut a jour aupres du fournisseur.
     */
    PaymentStatus lookupStatus(MobileMoneyProvider provider, String gatewayReference);
}
