package cm.ftg.tontine.treasurer.distribution.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record AuctionBidRequest(
        @NotNull UUID memberId,
        @NotNull @DecimalMin(value = "0.01", message = "Le sacrifice doit etre positif") BigDecimal sacrificeAmount
) {
}
