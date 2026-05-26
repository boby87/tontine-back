package cm.ftg.tontine.auditor.dashboard.dto;

import cm.ftg.tontine.treasurer.cashbox.dto.CashBoxDto;
import cm.ftg.tontine.treasurer.cashbox.dto.CashMovementDto;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record AuditorDashboardDto(
        List<CashBoxDto> cashBoxes,
        BigDecimal totalBalance,
        ValidationsBlock validations,
        AnomaliesBlock anomalies,
        ControlsBlock controls,
        RecommendationsBlock recommendations,
        LoansBlock loans,
        List<CashMovementDto> recentMovements
) {

    public record ValidationsBlock(long financialPending, long opinionPending) {
    }

    public record AnomaliesBlock(long open, long severityHigh) {
    }

    public record ControlsBlock(long planned, LocalDate nextDue) {
    }

    public record RecommendationsBlock(long active, BigDecimal implementedRate) {
    }

    public record LoansBlock(long active, long overdue, BigDecimal totalOutstanding) {
    }
}
