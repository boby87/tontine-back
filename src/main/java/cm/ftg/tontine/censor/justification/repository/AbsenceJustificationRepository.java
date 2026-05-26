package cm.ftg.tontine.censor.justification.repository;

import cm.ftg.tontine.censor.justification.entity.AbsenceJustification;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AbsenceJustificationRepository extends JpaRepository<AbsenceJustification, UUID> {

    List<AbsenceJustification> findByTontineIdOrderBySubmittedAtDesc(UUID tontineId);

    long countByTontineIdAndStatusIn(UUID tontineId,
            java.util.List<cm.ftg.tontine.censor.justification.enums.JustificationStatus> statuses);
}
