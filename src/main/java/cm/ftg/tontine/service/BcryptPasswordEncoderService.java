package cm.ftg.tontine.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Implémentation BCrypt (coût 12) du hachage de mots de passe.
 * Conforme aux copilot-instructions : BCrypt coût ≥ 12.
 */
@Service
public class BcryptPasswordEncoderService implements PasswordEncoderService {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

    @Override
    public String encode(String rawPassword) {
        return encoder.encode(rawPassword);
    }
}
