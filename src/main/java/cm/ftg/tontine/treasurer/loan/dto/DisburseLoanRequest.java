package cm.ftg.tontine.treasurer.loan.dto;

import cm.ftg.tontine.treasurer.common.enums.PaymentMethod;

public record DisburseLoanRequest(
        PaymentMethod paymentMethod
) {
}
