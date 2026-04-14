package cm.ftg.tontine.repository;

import cm.ftg.tontine.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, String> {

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);
}
