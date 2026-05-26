package cm.ftg.tontine.treasurer.cashbox.dto;

import cm.ftg.tontine.treasurer.cashbox.entity.CashBoxTransfer;
import cm.ftg.tontine.treasurer.cashbox.enums.CashTransferStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CashBoxTransferDto(
        UUID id,
        UUID tontineId,
        UUID fromCashBoxId,
        String fromCashBoxName,
        UUID toCashBoxId,
        String toCashBoxName,
        BigDecimal amount,
        String justification,
        CashTransferStatus status,
        Instant presidentApprovedAt,
        Instant auditorApprovedAt,
        Instant completedAt,
        Instant rejectedAt,
        String rejectionReason,
        String requestedByFullName,
        Instant requestedAt
) {

    public static CashBoxTransferDto from(CashBoxTransfer t) {
        return new CashBoxTransferDto(t.getId(), t.getTontineId(), t.getFromCashBoxId(), t.getFromCashBoxName(),
                t.getToCashBoxId(), t.getToCashBoxName(), t.getAmount(), t.getJustification(), t.getStatus(),
                t.getPresidentApprovedAt(), t.getAuditorApprovedAt(), t.getCompletedAt(), t.getRejectedAt(),
                t.getRejectionReason(), t.getRequestedByFullName(), t.getRequestedAt());
    }
}
