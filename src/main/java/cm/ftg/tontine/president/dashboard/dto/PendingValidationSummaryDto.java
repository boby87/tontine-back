package cm.ftg.tontine.president.dashboard.dto;

import cm.ftg.tontine.common.enums.Priority;
import cm.ftg.tontine.president.validation.entity.ValidationItem;
import cm.ftg.tontine.president.validation.enums.ValidationCategory;
import java.math.BigDecimal;
import java.util.UUID;

public record PendingValidationSummaryDto(
        UUID id,
        String title,
        ValidationCategory category,
        Priority priority,
        BigDecimal amount
) {

    public static PendingValidationSummaryDto from(ValidationItem v) {
        return new PendingValidationSummaryDto(v.getId(), v.getTitle(), v.getCategory(),
                v.getPriority(), v.getAmount());
    }
}
