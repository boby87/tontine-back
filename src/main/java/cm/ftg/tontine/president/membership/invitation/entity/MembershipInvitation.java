package cm.ftg.tontine.president.membership.invitation.entity;

import cm.ftg.tontine.common.enums.UserRole;
import cm.ftg.tontine.president.membership.invitation.enums.InvitationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(
        name = "membership_invitations",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_invitation_token", columnNames = "token")
        },
        indexes = {
                @Index(name = "idx_invitation_tontine", columnList = "tontine_id"),
                @Index(name = "idx_invitation_status", columnList = "status"),
                @Index(name = "idx_invitation_phone", columnList = "candidate_phone")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class MembershipInvitation {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tontine_id", nullable = false)
    private UUID tontineId;

    @Column(name = "candidate_full_name", nullable = false, length = 160)
    private String candidateFullName;

    @Column(name = "candidate_phone", nullable = false, length = 20)
    private String candidatePhone;

    @Column(name = "candidate_email", length = 160)
    private String candidateEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "proposed_role", nullable = false, length = 20)
    private UserRole proposedRole;

    /** Concat CSV "SMS,EMAIL,WHATSAPP". */
    @Column(nullable = false, length = 100)
    private String channels;

    @Column(length = 1000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvitationStatus status = InvitationStatus.PENDING;

    /**
     * Token public utilise dans le lien d'acceptation.
     * 24 octets SecureRandom -> Base64 URL-safe sans padding (~32 caracteres).
     */
    @Column(nullable = false, length = 80)
    private String token;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "accepted_user_id")
    private UUID acceptedUserId;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "invited_by_user_id", nullable = false)
    private UUID invitedByUserId;

    @Column(name = "invited_by_full_name", nullable = false, length = 160)
    private String invitedByFullName;

    @CreationTimestamp
    @Column(name = "invited_at", updatable = false, nullable = false)
    private Instant invitedAt;

    @Column(name = "reminders_sent", nullable = false)
    private int remindersSent = 0;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancelled_by_user_id")
    private UUID cancelledByUserId;

    @Column(name = "cancel_reason", length = 500)
    private String cancelReason;

    @Version
    private Long version;
}
