package cm.ftg.tontine.president.session.repository;

import cm.ftg.tontine.president.session.entity.Session;
import cm.ftg.tontine.president.session.enums.SessionStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SessionRepository extends JpaRepository<Session, UUID> {

    List<Session> findByTontineIdAndCycleIdOrderByNumberAsc(UUID tontineId, UUID cycleId);

    List<Session> findByTontineIdOrderByNumberAsc(UUID tontineId);

    List<Session> findByStatusAndScheduledAtBetween(SessionStatus status, Instant from, Instant to);

    List<Session> findByStatusAndScheduledAtBefore(SessionStatus status, Instant cutoff);

    @Query("SELECT MAX(s.number) FROM Session s WHERE s.cycleId = :cycleId")
    Optional<Integer> findMaxNumberByCycleId(@Param("cycleId") UUID cycleId);
}
