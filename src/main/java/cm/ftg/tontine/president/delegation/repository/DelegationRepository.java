package cm.ftg.tontine.president.delegation.repository;

import cm.ftg.tontine.president.delegation.entity.Delegation;
import cm.ftg.tontine.president.delegation.enums.DelegationStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DelegationRepository extends JpaRepository<Delegation, UUID> {

    List<Delegation> findByTontineIdOrderByCreatedAtDesc(UUID tontineId);

    List<Delegation> findByStatusAndEndsAtBefore(DelegationStatus status, Instant cutoff);
}
