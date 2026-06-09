package cm.ftg.tontine.auth.dto;

import cm.ftg.tontine.auth.entity.UserEntity;
import cm.ftg.tontine.common.enums.UserRole;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record UserDto(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String phone,
        String avatarUrl,
        Set<UserRole> roles,
        @JsonProperty("isActive") boolean active,
        @JsonProperty("isPhoneVerified") boolean phoneVerified,
        @JsonProperty("isEmailVerified") boolean emailVerified,
        Instant createdAt,
        Instant updatedAt
) {

    public static UserDto from(UserEntity u) {
        return from(u, u.getRoles());
    }

    /**
     * Construit un UserDto avec un ensemble de rôles agrégé (typiquement
     * User.roles + roles de chaque Membership actif). Permet au frontend
     * d'afficher les menus liés aux rôles tontine sans appel supplémentaire.
     */
    public static UserDto from(UserEntity u, Set<UserRole> aggregatedRoles) {
        return new UserDto(
                u.getId(),
                u.getFirstName(),
                u.getLastName(),
                u.getEmail(),
                u.getPhone(),
                u.getAvatarUrl(),
                aggregatedRoles,
                u.isActive(),
                u.isPhoneVerified(),
                u.isEmailVerified(),
                u.getCreatedAt(),
                u.getUpdatedAt());
    }
}
