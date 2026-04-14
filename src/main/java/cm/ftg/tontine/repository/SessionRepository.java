package cm.ftg.tontine.repository;

import cm.ftg.tontine.domain.Session;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionRepository extends JpaRepository<Session, Long> {

    int countByTontineId(Long tontineId);
}
