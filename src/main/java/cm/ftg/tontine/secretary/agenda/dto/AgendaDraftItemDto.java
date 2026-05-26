package cm.ftg.tontine.secretary.agenda.dto;

import cm.ftg.tontine.secretary.agenda.entity.AgendaDraftItem;
import java.util.UUID;

public record AgendaDraftItemDto(
        UUID id,
        int order,
        String title,
        String description,
        boolean isStandard,
        Integer estimatedDurationMin,
        String proposedBy
) {

    public static AgendaDraftItemDto from(AgendaDraftItem i) {
        return new AgendaDraftItemDto(
                i.getId(),
                i.getOrderIdx(),
                i.getTitle(),
                i.getDescription(),
                i.isStandard(),
                i.getEstimatedDurationMin(),
                i.getProposedBy());
    }
}
