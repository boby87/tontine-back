package cm.ftg.tontine.secretary.agenda.repository;

import cm.ftg.tontine.secretary.agenda.entity.AgendaDraft;
import cm.ftg.tontine.secretary.agenda.enums.AgendaDraftStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AgendaDraftRepository extends JpaRepository<AgendaDraft, UUID> {

    List<AgendaDraft> findByTontineIdOrderByCreatedAtDesc(UUID tontineId);

    List<AgendaDraft> findByTontineIdAndSessionIdOrderByCreatedAtDesc(UUID tontineId, UUID sessionId);

    List<AgendaDraft> findByTontineIdAndStatusOrderByCreatedAtDesc(UUID tontineId, AgendaDraftStatus status);

    List<AgendaDraft> findByTontineIdAndStatusAndSessionIdOrderByCreatedAtDesc(
            UUID tontineId, AgendaDraftStatus status, UUID sessionId);

    long countByTontineIdAndStatus(UUID tontineId, AgendaDraftStatus status);

    Optional<AgendaDraft> findFirstByTontineIdAndSessionIdAndStatusInOrderByApprovedAtDesc(
            UUID tontineId, UUID sessionId, Collection<AgendaDraftStatus> statuses);

    @Query("SELECT COALESCE(MAX(a.sessionNumber), 0) + 1 FROM AgendaDraft a WHERE a.tontineId = :tontineId")
    int findNextSessionNumber(@Param("tontineId") UUID tontineId);
}
