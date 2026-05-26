package cm.ftg.tontine.president.common;

import cm.ftg.tontine.auth.repository.UserRepository;
import cm.ftg.tontine.common.exception.ApiException;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class TontineIdResolver {

    private final UserRepository userRepository;

    public TontineIdResolver(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UUID resolve(UUID userId, UUID headerTontineId) {
        if (headerTontineId != null) {
            return headerTontineId;
        }
        UUID active = userRepository.findById(userId)
                .map(u -> u.getActiveTontineId())
                .orElse(null);
        if (active == null) {
            throw new ApiException("TONTINE_REQUIRED",
                    "Tontine non determinee. Fournir l'en-tete X-Tontine-Id ou definir une tontine active.",
                    HttpStatus.BAD_REQUEST);
        }
        return active;
    }
}
