package cm.ftg.tontine.member.repository;

import cm.ftg.tontine.member.entity.Member;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MemberRepository extends JpaRepository<Member, UUID> {

    List<Member> findByUserId(UUID userId);

    Optional<Member> findByUserIdAndTontineId(UUID userId, UUID tontineId);

    List<Member> findByTontineId(UUID tontineId);

    long countByTontineId(UUID tontineId);

    long countByTontineIdAndStatus(UUID tontineId, cm.ftg.tontine.common.enums.MemberStatus status);

    List<Member> findByTontineIdOrderByMatriculeAsc(UUID tontineId);
}
