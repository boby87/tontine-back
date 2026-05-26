package cm.ftg.tontine.integration.mobilemoney.webhook;

import cm.ftg.tontine.treasurer.mobilemoney.enums.MobileMoneyProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;

public record MobileMoneyWebhookPayload(
        @NotNull UUID tontineId,
        @NotNull MobileMoneyProvider provider,
        @NotBlank String fromPhone,
        @NotNull @Positive BigDecimal amount,
        @NotBlank String externalReference,
        String memberReference) {
}
