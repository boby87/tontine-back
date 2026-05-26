package cm.ftg.tontine.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(max = 80) String firstName,
        @NotBlank @Size(max = 80) String lastName,
        @NotBlank @Pattern(regexp = "^\\+[1-9]\\d{6,14}$",
                message = "Le telephone doit etre au format E.164 (+237...)") String phone,
        @NotBlank @Email @Size(max = 160) String email,
        @NotBlank @Size(min = 8, max = 100, message = "Le mot de passe doit faire au moins 8 caracteres") String password
) {
}
