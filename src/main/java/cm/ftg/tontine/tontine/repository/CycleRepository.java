package cm.ftg.tontine.tontine.repository;

import cm.ftg.tontine.tontine.entity.Cycle;
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

    @Query("SELECT MAX(c.number) FROM Cycle c WHERE c.tontineId = :tontineId")
    Optional<Integer> findMaxNumberByTontineId(@Param("tontineId") UUID tontineId);
}
