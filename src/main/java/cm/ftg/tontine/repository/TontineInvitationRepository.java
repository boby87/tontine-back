package cm.ftg.tontine.repository;

import cm.ftg.tontine.domain.TontineInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface TontineInvitationRepository extends JpaRepository<TontineInvitation, String> {

    Optional<TontineInvitation> findByTokenHash(String tokenHash);

    /**
     * Supprime physiquement les invitations expirées.
     * Appelé périodiquement par le cleanup scheduler.
     *
     * @return le nombre d'invitations supprimées
     */
    @Modifying
    @Query("DELETE FROM TontineInvitation i WHERE i.expiresAt < :now")
    int deleteExpiredBefore(@Param("now") LocalDateTime now);
}

