package cm.ftg.tontine.president.cycleclose.repository;

import cm.ftg.tontine.president.cycleclose.entity.CycleClose;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CycleCloseRepository extends JpaRepository<CycleClose, UUID> {

    Optional<CycleClose> findByTontineIdAndCycleId(UUID tontineId, UUID cycleId);
}
