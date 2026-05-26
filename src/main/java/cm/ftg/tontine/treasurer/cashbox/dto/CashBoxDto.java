package cm.ftg.tontine.treasurer.cashbox.dto;

import cm.ftg.tontine.treasurer.cashbox.entity.CashBox;
import cm.ftg.tontine.treasurer.cashbox.enums.CashBoxType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CashBoxDto(
        UUID id,
        UUID tontineId,
        String name,
        CashBoxType type,
        BigDecimal balance,
        boolean isLocked,
        BigDecimal thresholdMin,
        Instant createdAt
) {

    public static CashBoxDto from(CashBox b) {
        return new CashBoxDto(b.getId(), b.getTontineId(), b.getName(), b.getType(),
                b.getBalance(), b.isLocked(), b.getThresholdMin(), b.getCreatedAt());
    }
}
