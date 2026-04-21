package cm.ftg.tontine.service;

import cm.ftg.tontine.exception.AuthentificationEchoueeException;
import cm.ftg.tontine.repository.UserRepository;
import cm.ftg.tontine.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service d'authentification — vérifie les identifiants et génère un token JWT.
 *
 * <p>Conformément à SKILL.md §1, §4, §5 :</p>
 * <ul>
 *   <li>Ne jamais révéler si c'est l'email ou le mot de passe qui est incorrect</li>
 *   <li>Masquer les données sensibles dans les logs</li>
 *   <li>Journaliser les échecs d'authentification pour l'audit</li>
 * </ul>
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final Logger securityLog = LoggerFactory.getLogger("SECURITY_AUDIT");

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);

    public AuthService(UserRepository userRepository,
                       JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * Authentifie un utilisateur par email + mot de passe et retourne un token JWT.
     *
     * @param request email et mot de passe
     * @return résultat contenant le token et les infos utilisateur
     * @throws AuthentificationEchoueeException si l'authentification échoue
     */
    @Transactional(readOnly = true)
    public AuthResult authentifier(LoginRequest request) {
        var user = userRepository.findByEmail(request.email())
            .filter(u -> u.getDeletedAt() == null)
            .orElse(null);

        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            securityLog.warn("AUTH_FAILURE [email=***, cause=bad_credentials]");
            throw new AuthentificationEchoueeException();
        }

        var token = jwtTokenProvider.generateToken(
            user.getId(),
            user.getEmail(),
            user.getRole().name()
        );

        log.info("Authentification réussie [userId={}, role={}] — VirtualThread={}",
            user.getId(), user.getRole(), Thread.currentThread().isVirtual());

        return new AuthResult(
            token,
            user.getId(),
            user.getEmail(),
            user.getRole().name()
        );
    }
}

