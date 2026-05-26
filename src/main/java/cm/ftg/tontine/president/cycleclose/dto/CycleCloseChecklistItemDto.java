package cm.ftg.tontine.president.cycleclose.dto;

import cm.ftg.tontine.president.cycleclose.entity.CycleCloseChecklistItem;
import cm.ftg.tontine.president.cycleclose.enums.ChecklistItemStatus;

public record CycleCloseChecklistItemDto(
        String key,
        String label,
        ChecklistItemStatus status,
        String blockingReason
) {

    public static CycleCloseChecklistItemDto from(CycleCloseChecklistItem i) {
        return new CycleCloseChecklistItemDto(i.getItemKey(), i.getLabel(), i.getStatus(), i.getBlockingReason());
    }
}
