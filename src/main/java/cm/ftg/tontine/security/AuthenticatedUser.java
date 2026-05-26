package cm.ftg.tontine.security;

import cm.ftg.tontine.auth.entity.UserEntity;
import cm.ftg.tontine.common.enums.UserRole;
import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public record AuthenticatedUser(UUID id, String email, String phone, String passwordHash,
                                java.util.Set<UserRole> roles, boolean active) implements UserDetails {

    public static AuthenticatedUser from(UserEntity user) {
        return new AuthenticatedUser(
                user.getId(),
                user.getEmail(),
                user.getPhone(),
                user.getPasswordHash(),
                user.getRoles(),
                user.isActive());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r.name()))
                .collect(Collectors.toSet());
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return id.toString();
    }

    @Override
    public boolean isAccountNonExpired() {
        return active;
    }

    @Override
    public boolean isAccountNonLocked() {
        return active;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
