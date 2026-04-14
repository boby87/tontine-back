package cm.ftg.tontine.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Utilisateur de la plateforme.
 * CDC Section 7.1 — entité User.
 * Soft delete via deletedAt (ne jamais supprimer physiquement).
 */
@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(unique = true, nullable = false)
    private String phone;                           // Format camerounais +237XXXXXXXXX — unique

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;                    // BCrypt coût ≥ 12

    @Column(name = "first_name", nullable = false)
    private String firstName;                       // Min 2 caractères (valider en service)

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;                  // Âge ≥ 18 ans (valider en service)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role = UserRole.MEMBER;         // Rôle plateforme (ADMIN, MEMBER, …)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Region region;                          // 10 régions du Cameroun

    @Column(name = "cni_number", nullable = false)
    private String cniNumber;

    @Column(name = "cni_photo_url")
    private String cniPhotoUrl;

    @Column(name = "profile_photo_url")
    private String profilePhotoUrl;

    @Column(name = "mobile_money_number")
    private String mobileMoneyNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "mobile_money_provider")
    private MobileMoneyProvider mobileMoneyProvider; // MTN / ORANGE

    @Column(name = "is_verified", nullable = false)
    private boolean verified = false;

    @Column(name = "is_2fa_enabled", nullable = false)
    private boolean twoFactorEnabled = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;                // Soft delete

    // ── Relation inverse : adhésions de cet utilisateur à des tontines ──
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TontineMember> memberships = new ArrayList<>();

    // ── Accessors ──

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }
    public Region getRegion() { return region; }
    public void setRegion(Region region) { this.region = region; }
    public String getCniNumber() { return cniNumber; }
    public void setCniNumber(String cniNumber) { this.cniNumber = cniNumber; }
    public String getCniPhotoUrl() { return cniPhotoUrl; }
    public void setCniPhotoUrl(String cniPhotoUrl) { this.cniPhotoUrl = cniPhotoUrl; }
    public String getProfilePhotoUrl() { return profilePhotoUrl; }
    public void setProfilePhotoUrl(String profilePhotoUrl) { this.profilePhotoUrl = profilePhotoUrl; }
    public String getMobileMoneyNumber() { return mobileMoneyNumber; }
    public void setMobileMoneyNumber(String mobileMoneyNumber) { this.mobileMoneyNumber = mobileMoneyNumber; }
    public MobileMoneyProvider getMobileMoneyProvider() { return mobileMoneyProvider; }
    public void setMobileMoneyProvider(MobileMoneyProvider p) { this.mobileMoneyProvider = p; }
    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }
    public boolean isTwoFactorEnabled() { return twoFactorEnabled; }
    public void setTwoFactorEnabled(boolean twoFactorEnabled) { this.twoFactorEnabled = twoFactorEnabled; }
    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }
    public List<TontineMember> getMemberships() { return memberships; }
    public void setMemberships(List<TontineMember> memberships) { this.memberships = memberships; }
}
