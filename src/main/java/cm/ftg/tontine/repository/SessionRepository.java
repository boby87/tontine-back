package cm.ftg.tontine.repository;

import cm.ftg.tontine.domain.Session;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface SessionRepository extends JpaRepository<Session, Long> {

    int countByTontineId(Long tontineId);

    /**
     * Prochaine date de séance SCHEDULED pour une tontine donnée.
     * Utiliser {@code PageRequest.of(0, 1)} pour limiter à 1 résultat.
     */
    @Query("SELECT s.scheduledDate FROM Session s "
         + "WHERE s.tontine.id = :tontineId "
         + "AND s.status = cm.ftg.tontine.domain.SessionStatus.SCHEDULED "
         + "ORDER BY s.scheduledDate ASC")
    List<LocalDate> findNextScheduledDate(@Param("tontineId") Long tontineId, Pageable pageable);

    /**
     * Nombre de séances COMPLETED pour une tontine donnée.
     */
    @Query("SELECT COUNT(s) FROM Session s "
         + "WHERE s.tontine.id = :tontineId "
         + "AND s.status = cm.ftg.tontine.domain.SessionStatus.COMPLETED")
    int countCompletedSessions(@Param("tontineId") Long tontineId);
}
