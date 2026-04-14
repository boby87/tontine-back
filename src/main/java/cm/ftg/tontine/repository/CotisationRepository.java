package cm.ftg.tontine.repository;

import cm.ftg.tontine.domain.Cotisation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CotisationRepository extends JpaRepository<Cotisation, Long> {
    boolean existsByMembreIdAndSeanceId(Long membreId, Long seanceId);
}
