package cm.ftg.tontine.auditor.audit.repository;

import cm.ftg.tontine.auditor.audit.entity.Audit;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditRepository extends JpaRepository<Audit, UUID> {

    List<Audit> findByTontineIdOrderByCreatedAtDesc(UUID tontineId);
}
