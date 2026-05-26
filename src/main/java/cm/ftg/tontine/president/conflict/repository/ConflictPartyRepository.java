package cm.ftg.tontine.president.conflict.repository;

import cm.ftg.tontine.president.conflict.entity.ConflictParty;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConflictPartyRepository extends JpaRepository<ConflictParty, UUID> {

    List<ConflictParty> findByConflictIdOrderByFullNameAsc(UUID conflictId);
}
