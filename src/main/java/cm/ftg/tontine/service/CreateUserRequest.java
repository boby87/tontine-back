package cm.ftg.tontine.service;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

/**
 * Requête d'inscription d'un utilisateur.
 * Validée côté contrôleur via @Valid.
 */
public record CreateUserRequest(

    @NotBlank @Email
    String email,

    @NotBlank @Pattern(regexp = "^\\+237[0-9]{9}$", message = "Format attendu : +237XXXXXXXXX")
    String phone,

    @NotBlank @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
    String password,

    @NotBlank @Size(min = 2)
    String firstName,

    @NotBlank @Size(min = 2)
    String lastName,

    @NotNull @Past
    LocalDate dateOfBirth,

    @NotNull
    String region,

    @NotBlank
    String cniNumber
) {}
