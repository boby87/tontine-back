package cm.ftg.tontine.repository;

import cm.ftg.tontine.domain.Sanction;
import cm.ftg.tontine.domain.SanctionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SanctionRepository extends JpaRepository<Sanction, String> {

    boolean existsByCleIdempotence(String cleIdempotence);

    List<Sanction> findByMemberIdAndStatus(String memberId, SanctionStatus status);

    List<Sanction> findBySessionId(Long sessionId);

    /** Nombre de sanctions avec un statut donné pour une tontine (via member → tontine) */
    int countByMember_Tontine_IdAndStatus(Long tontineId, SanctionStatus status);
}
