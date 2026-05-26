package cm.ftg.tontine.censor.communication.dto;

import cm.ftg.tontine.censor.communication.entity.CensorCommunication;
import cm.ftg.tontine.censor.communication.enums.CommunicationKind;
import cm.ftg.tontine.common.enums.NotificationChannel;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record CensorCommunicationDto(
        UUID id,
        UUID tontineId,
        CommunicationKind kind,
        String subject,
        String body,
        Set<NotificationChannel> channels,
        List<UUID> recipientMemberIds,
        List<UUID> relatedSanctionIds,
        UUID sentByUserId,
        String sentByFullName,
        Instant sentAt,
        int recipientsCount
) {

    public static CensorCommunicationDto from(CensorCommunication c) {
        return new CensorCommunicationDto(
                c.getId(), c.getTontineId(), c.getKind(), c.getSubject(), c.getBody(),
                c.getChannels(), c.getRecipientMemberIds(), c.getRelatedSanctionIds(),
                c.getSentByUserId(), c.getSentByFullName(), c.getSentAt(), c.getRecipientsCount());
    }
}
