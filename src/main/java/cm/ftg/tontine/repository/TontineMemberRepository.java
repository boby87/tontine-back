package cm.ftg.tontine.repository;

import cm.ftg.tontine.domain.TontineMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TontineMemberRepository extends JpaRepository<TontineMember, String> {

    boolean existsByTontineIdAndUserId(Long tontineId, String userId);

    Optional<TontineMember> findByTontineIdAndUserId(Long tontineId, String userId);

    /** Retrouve toutes les appartenances d'un utilisateur donné (toutes ses tontines) */
    List<TontineMember> findByUserId(String userId);
}
