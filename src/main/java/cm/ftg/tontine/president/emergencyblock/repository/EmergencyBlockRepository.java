package cm.ftg.tontine.president.emergencyblock.repository;

import cm.ftg.tontine.president.emergencyblock.entity.EmergencyBlock;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EmergencyBlockRepository extends JpaRepository<EmergencyBlock, UUID> {

    @Query("SELECT b FROM EmergencyBlock b WHERE b.tontineId = :tontineId "
            + "ORDER BY CASE b.status WHEN cm.ftg.tontine.president.emergencyblock.enums.EmergencyBlockStatus.ACTIVE THEN 0 ELSE 1 END, "
            + "b.activatedAt DESC")
    List<EmergencyBlock> findByTontineIdOrdered(@Param("tontineId") UUID tontineId);
}
