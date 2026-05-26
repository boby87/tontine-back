package cm.ftg.tontine.auth.repository;

import cm.ftg.tontine.auth.entity.UserEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByEmail(String email);

    Optional<UserEntity> findByPhone(String phone);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    default Optional<UserEntity> findByIdentifier(String identifier) {
        return findByEmail(identifier).or(() -> findByPhone(identifier));
    }
}
