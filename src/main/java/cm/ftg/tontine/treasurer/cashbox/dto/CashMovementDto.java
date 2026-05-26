package cm.ftg.tontine.treasurer.cashbox.dto;

import cm.ftg.tontine.treasurer.cashbox.entity.CashMovement;
import cm.ftg.tontine.treasurer.cashbox.enums.CashMovementKind;
import cm.ftg.tontine.treasurer.cashbox.enums.MovementDirection;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CashMovementDto(
        UUID id,
        UUID tontineId,
        UUID cashBoxId,
        String cashBoxName,
        CashMovementKind kind,
        BigDecimal amount,
        MovementDirection direction,
        String reference,
        String description,
        BigDecimal balanceAfter,
        String recordedByFullName,
        Instant recordedAt
) {

    public static CashMovementDto from(CashMovement m) {
        return new CashMovementDto(m.getId(), m.getTontineId(), m.getCashBoxId(), m.getCashBoxName(),
                m.getKind(), m.getAmount(), m.getDirection(), m.getReference(), m.getDescription(),
                m.getBalanceAfter(), m.getRecordedByFullName(), m.getRecordedAt());
    }
}
