package cm.ftg.tontine.auditor.control.repository;

import cm.ftg.tontine.auditor.control.entity.Control;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ControlRepository extends JpaRepository<Control, UUID> {

    List<Control> findByTontineIdOrderByCreatedAtDesc(UUID tontineId);
}
