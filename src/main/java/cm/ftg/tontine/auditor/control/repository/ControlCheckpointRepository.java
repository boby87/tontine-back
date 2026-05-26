package cm.ftg.tontine.auditor.control.repository;

import cm.ftg.tontine.auditor.control.entity.ControlCheckpoint;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ControlCheckpointRepository extends JpaRepository<ControlCheckpoint, UUID> {

    List<ControlCheckpoint> findByControlIdOrderByOrderIdxAsc(UUID controlId);

    List<ControlCheckpoint> findByControlIdInOrderByOrderIdxAsc(List<UUID> controlIds);

    void deleteByControlId(UUID controlId);
}
