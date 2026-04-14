package cm.ftg.tontine.repository;

import cm.ftg.tontine.domain.Tontine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TontineRepository extends JpaRepository<Tontine, Long> {

    boolean existsByNom(String nom);

    Optional<Tontine> findByIdAndDeletedAtIsNull(Long id);
}
