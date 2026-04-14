package cm.ftg.tontine.repository;

import cm.ftg.tontine.domain.Membre;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MembreRepository extends JpaRepository<Membre, Long> {
    Optional<Membre> findByIdAndTontineId(Long id, Long tontineId);
}
