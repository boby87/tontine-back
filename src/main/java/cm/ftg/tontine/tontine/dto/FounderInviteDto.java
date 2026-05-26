package cm.ftg.tontine.tontine.dto;

import cm.ftg.tontine.common.enums.UserRole;
import cm.ftg.tontine.tontine.entity.FounderInviteEmbeddable;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record FounderInviteDto(
        @NotBlank @Size(max = 160) String fullName,
        @NotBlank @Pattern(regexp = "^\\+[1-9]\\d{6,14}$") String phone,
        @Email String email,
        @NotNull UserRole role
) {

    public FounderInviteEmbeddable toEmbeddable() {
        return new FounderInviteEmbeddable(fullName, phone, email, role);
    }

    public static FounderInviteDto from(FounderInviteEmbeddable f) {
        return new FounderInviteDto(f.getFullName(), f.getPhone(), f.getEmail(), f.getRole());
    }
}
