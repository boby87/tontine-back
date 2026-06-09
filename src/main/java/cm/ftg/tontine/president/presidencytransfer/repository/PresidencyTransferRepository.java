package cm.ftg.tontine.president.presidencytransfer.repository;

import cm.ftg.tontine.president.presidencytransfer.entity.PresidencyTransfer;
import cm.ftg.tontine.president.presidencytransfer.enums.PresidencyTransferStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PresidencyTransferRepository extends JpaRepository<PresidencyTransfer, UUID> {

    Optional<PresidencyTransfer> findFirstByTontineIdAndStatus(UUID tontineId, PresidencyTransferStatus status);

    Optional<PresidencyTransfer> findFirstByTargetUserIdAndStatus(UUID targetUserId, PresidencyTransferStatus status);

    List<PresidencyTransfer> findByTontineIdOrderByInitiatedAtDesc(UUID tontineId);

    List<PresidencyTransfer> findByStatusAndExpiresAtBefore(PresidencyTransferStatus status, Instant moment);
}
