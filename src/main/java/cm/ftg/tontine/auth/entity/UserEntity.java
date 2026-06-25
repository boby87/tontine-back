package cm.ftg.tontine.auth.entity;

import cm.ftg.tontine.common.entity.BaseEntity;
import cm.ftg.tontine.common.enums.UserRole;
import cm.ftg.tontine.common.enums.UserStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(name = "uk_users_email", columnNames = "email"),
        @UniqueConstraint(name = "uk_users_phone", columnNames = "phone")
})
@Getter
@Setter
@NoArgsConstructor
public class UserEntity extends BaseEntity {

    @Column(nullable = false, length = 80)
    private String firstName;

    @Column(nullable = false, length = 80)
    private String lastName;

    @Column(nullable = false, length = 160)
    private String email;

    @Column(nullable = false, length = 20)
    private String phone;

    @JsonIgnore
    @Column(name = "password_hash", nullable = false, length = 100)
    @Getter(AccessLevel.NONE)
    private String passwordHash;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    /** Une des 10 régions du Cameroun. */
    @Column(length = 60)
    private String region;

    @Column(name = "cni_number", length = 40)
    private String cniNumber;

    @Column(name = "cni_photo_url", length = 512)
    private String cniPhotoUrl;

    @Column(name = "profile_photo_url", length = 512)
    private String profilePhotoUrl;

    /** Numéro Mobile Money chiffré AES-256 en base. */
    @Column(name = "mobile_money_number", length = 512)
    private String mobileMoneyNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "mobile_money_provider", length = 20)
    private MobileMoneyProvider mobileMoneyProvider;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    @Column(name = "phone_verified", nullable = false)
    private boolean phoneVerified = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserStatus status = UserStatus.PENDING_VERIFICATION;

    @Column(name = "is_2fa_enabled", nullable = false)
    private boolean is2faEnabled = false;

    @Column(name = "is_totp_enabled", nullable = false)
    private boolean isTotpEnabled = false;

    @Column(name = "totp_secret", length = 80)
    private String totpSecret;

    /** Rôles globaux — ADMIN uniquement pour le Super Administrateur plateforme.
     *  Les rôles métier (PRESIDENT, TREASURER…) vivent sur TontineMember. */
    @ElementCollection(fetch = FetchType.EAGER, targetClass = UserRole.class)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 30)
    private Set<UserRole> roles = EnumSet.noneOf(UserRole.class);

    /** Identifiant de la tontine active (dernière tontine sélectionnée par l'utilisateur). */
    @Column(name = "active_tontine_id")
    private java.util.UUID activeTontineId;

    @Version
    private Long version;

    @JsonIgnore
    public String getPasswordHash() {
        return passwordHash;
    }

    /** Rétro-compatibilité avec JwtAuthenticationFilter. */
    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    public enum MobileMoneyProvider {
        MTN, ORANGE
    }
}
