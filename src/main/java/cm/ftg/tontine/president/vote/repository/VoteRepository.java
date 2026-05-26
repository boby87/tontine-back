package cm.ftg.tontine.president.vote.repository;

import cm.ftg.tontine.president.vote.entity.Vote;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VoteRepository extends JpaRepository<Vote, UUID> {

    List<Vote> findByTontineIdOrderByCreatedAtDesc(UUID tontineId);
}
