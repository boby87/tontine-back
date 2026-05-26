package cm.ftg.tontine.censor.communication.dto;

import cm.ftg.tontine.censor.communication.enums.CommunicationKind;
import cm.ftg.tontine.common.enums.NotificationChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record CreateCommunicationRequest(
        @NotNull CommunicationKind kind,
        @NotBlank String subject,
        @NotBlank String body,
        @NotEmpty Set<NotificationChannel> channels,
        @NotEmpty List<UUID> recipientMemberIds,
        List<UUID> relatedSanctionIds
) {}
