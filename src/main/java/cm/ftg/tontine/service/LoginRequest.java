package cm.ftg.tontine.service;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Requête de connexion — email + mot de passe.
 * Le mot de passe n'est jamais loggé ni retourné (security-rules §4.1).
 */
public record LoginRequest(
    @NotBlank @Email String email,
    @NotBlank String password
) {}

