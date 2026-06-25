package cm.ftg.tontine.treasurer.common.enums;

public enum PaymentMethod {
    MTN_MOMO,
    ORANGE_MONEY,
    CASH,
    BANK_TRANSFER,
    /** @deprecated use MTN_MOMO */
    @Deprecated
    MOBILE_MONEY,
    CHECK
}
