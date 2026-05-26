package cm.ftg.tontine.auditor.audit.repository;

import cm.ftg.tontine.auditor.audit.entity.AuditFindingEntry;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditFindingEntryRepository extends JpaRepository<AuditFindingEntry, UUID> {

    List<AuditFindingEntry> findByAuditIdOrderByOrderIdxAsc(UUID auditId);

    List<AuditFindingEntry> findByAuditIdInOrderByOrderIdxAsc(List<UUID> auditIds);
}
