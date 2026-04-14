package cm.ftg.tontine.repository;

import cm.ftg.tontine.domain.Amende;
import cm.ftg.tontine.domain.StatutAmende;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AmendeRepository extends JpaRepository<Amende, Long> {
    boolean existsByMembreIdAndStatut(Long membreId, StatutAmende statut);
    boolean existsByMembreIdAndSeanceId(Long membreId, Long seanceId);
}
