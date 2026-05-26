package cm.ftg.tontine.president.dashboard.dto;

import java.util.List;

public record PresidentDashboardDto(
        PresidentKpiDto kpi,
        List<PresidentAlertDto> alerts,
        List<PendingValidationSummaryDto> pendingValidations,
        List<Object> performance,
        List<Object> recentDecisions,
        List<Object> agenda
) {
}
