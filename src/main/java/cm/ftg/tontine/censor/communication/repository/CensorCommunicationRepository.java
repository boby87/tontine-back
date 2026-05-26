package cm.ftg.tontine.censor.communication.repository;

import cm.ftg.tontine.censor.communication.entity.CensorCommunication;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CensorCommunicationRepository extends JpaRepository<CensorCommunication, UUID> {

    List<CensorCommunication> findByTontineIdOrderBySentAtDesc(UUID tontineId);
}
