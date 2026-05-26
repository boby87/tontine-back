package cm.ftg.tontine.auth.dto;

import cm.ftg.tontine.auth.entity.UserEntity;
import cm.ftg.tontine.common.enums.UserRole;
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
        boolean active,
        boolean phoneVerified,
        boolean emailVerified,
        Instant createdAt,
        Instant updatedAt
) {

    public static UserDto from(UserEntity u) {
        return new UserDto(
                u.getId(),
                u.getFirstName(),
                u.getLastName(),
                u.getEmail(),
                u.getPhone(),
                u.getAvatarUrl(),
                u.getRoles(),
                u.isActive(),
                u.isPhoneVerified(),
                u.isEmailVerified(),
                u.getCreatedAt(),
                u.getUpdatedAt());
    }
}
