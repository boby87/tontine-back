package cm.ftg.tontine.secretary.registry.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateMemberRegistryRequest(
        @Pattern(regexp = "^[+0-9 .-]{6,20}$", message = "Format telephone invalide") String phone,
        @Email @Size(max = 160) String email,
        @Size(max = 40) String matricule
) {
}
