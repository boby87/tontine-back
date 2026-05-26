package cm.ftg.tontine.integration.mobilemoney.gateway;

public record PaymentResult(
        String gatewayReference,
        PaymentStatus status,
        String providerMessage) {
}
