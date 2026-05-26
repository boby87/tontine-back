package cm.ftg.tontine.censor.communication.entity;

import cm.ftg.tontine.censor.communication.enums.CommunicationKind;
import cm.ftg.tontine.common.enums.NotificationChannel;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "censor_communications", indexes = {
        @Index(name = "idx_censor_comm_tontine", columnList = "tontine_id"),
        @Index(name = "idx_censor_comm_kind", columnList = "kind")
})
@Getter
@Setter
@NoArgsConstructor
public class CensorCommunication {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tontine_id", nullable = false)
    private UUID tontineId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CommunicationKind kind;

    @Column(nullable = false, length = 200)
    private String subject;

    @Column(nullable = false, length = 4000)
    private String body;

    @ElementCollection(fetch = FetchType.EAGER, targetClass = NotificationChannel.class)
    @CollectionTable(name = "censor_communication_channels",
            joinColumns = @JoinColumn(name = "communication_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    private Set<NotificationChannel> channels = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "censor_communication_recipients",
            joinColumns = @JoinColumn(name = "communication_id"))
    @Column(name = "member_id", nullable = false)
    private List<UUID> recipientMemberIds = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "censor_communication_sanctions",
            joinColumns = @JoinColumn(name = "communication_id"))
    @Column(name = "sanction_id", nullable = false)
    private List<UUID> relatedSanctionIds = new ArrayList<>();

    @Column(name = "sent_by_user_id", nullable = false)
    private UUID sentByUserId;

    @Column(name = "sent_by_full_name", length = 160)
    private String sentByFullName;

    @CreationTimestamp
    @Column(name = "sent_at", updatable = false, nullable = false)
    private Instant sentAt;

    @Column(name = "recipients_count", nullable = false)
    private int recipientsCount = 0;
}
