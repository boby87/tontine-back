package cm.ftg.tontine.service;

/**
 * Abstraction du hachage de mots de passe.
 * Implémentation par défaut : BCrypt via {@link BcryptPasswordEncoderService}.
 * Sera remplacé par Spring Security PasswordEncoder quand le starter-security sera ajouté.
 */
public interface PasswordEncoderService {

    /** Hache un mot de passe en clair. */
    String encode(String rawPassword);
}
