package cm.ftg.tontine.president.conflict.repository;

import cm.ftg.tontine.president.conflict.entity.Conflict;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConflictRepository extends JpaRepository<Conflict, UUID> {

    List<Conflict> findByTontineIdOrderByEscalatedAtDesc(UUID tontineId);
}
