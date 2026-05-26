package cm.ftg.tontine.president.session.dto;

import cm.ftg.tontine.president.session.entity.AgendaItem;
import cm.ftg.tontine.president.session.enums.AgendaItemStatus;
import java.util.UUID;

public record AgendaItemDto(
        UUID id,
        int order,
        String title,
        String description,
        AgendaItemStatus status
) {

    public static AgendaItemDto from(AgendaItem a) {
        return new AgendaItemDto(a.getId(), a.getOrderIdx(), a.getTitle(), a.getDescription(), a.getStatus());
    }
}
