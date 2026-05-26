package cm.ftg.tontine.auditor.anomaly.repository;

import cm.ftg.tontine.auditor.anomaly.entity.Anomaly;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnomalyRepository extends JpaRepository<Anomaly, UUID> {

    List<Anomaly> findByTontineIdOrderByReportedAtDesc(UUID tontineId);
}
