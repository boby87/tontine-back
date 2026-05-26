package cm.ftg.tontine.auditor.clarification.repository;

import cm.ftg.tontine.auditor.clarification.entity.Clarification;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClarificationRepository extends JpaRepository<Clarification, UUID> {

    List<Clarification> findByTontineIdOrderByAskedAtDesc(UUID tontineId);
}
