package cm.ftg.tontine.secretary.agenda.repository;

import cm.ftg.tontine.secretary.agenda.entity.AgendaDraft;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AgendaDraftRepository extends JpaRepository<AgendaDraft, UUID> {

    List<AgendaDraft> findByTontineIdOrderByCreatedAtDesc(UUID tontineId);

    long countByTontineIdAndStatus(UUID tontineId,
            cm.ftg.tontine.secretary.agenda.enums.AgendaDraftStatus status);

    @Query("SELECT COALESCE(MAX(a.sessionNumber), 0) + 1 FROM AgendaDraft a WHERE a.tontineId = :tontineId")
    int findNextSessionNumber(@Param("tontineId") UUID tontineId);
}
