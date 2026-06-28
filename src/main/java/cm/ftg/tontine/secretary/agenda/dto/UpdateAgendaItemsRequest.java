package cm.ftg.tontine.secretary.agenda.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record UpdateAgendaItemsRequest(
        @NotNull @Valid List<CreateAgendaDraftItemRequest> items
) {}
