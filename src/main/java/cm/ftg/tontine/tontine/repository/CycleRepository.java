package cm.ftg.tontine.tontine.repository;

import cm.ftg.tontine.tontine.entity.Cycle;
import cm.ftg.tontine.tontine.enums.CycleStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CycleRepository extends JpaRepository<Cycle, UUID> {

    List<Cycle> findByTontineIdOrderByNumberAsc(UUID tontineId);

    Optional<Cycle> findByTontineIdAndStatus(UUID tontineId, CycleStatus status);

    boolean existsByTontineIdAndStatusIn(UUID tontineId, List<CycleStatus> statuses);

    List<Cycle> findByStatusOrderByTontineIdAscNumberAsc(CycleStatus status);

    @Query("SELECT MAX(c.number) FROM Cycle c WHERE c.tontineId = :tontineId")
    Optional<Integer> findMaxNumberByTontineId(@Param("tontineId") UUID tontineId);
}
