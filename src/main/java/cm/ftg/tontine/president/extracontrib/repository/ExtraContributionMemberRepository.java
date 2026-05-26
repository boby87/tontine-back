package cm.ftg.tontine.president.extracontrib.repository;

import cm.ftg.tontine.president.extracontrib.entity.ExtraContributionMember;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExtraContributionMemberRepository extends JpaRepository<ExtraContributionMember, UUID> {

    List<ExtraContributionMember> findByExtraContribIdOrderByFullNameAsc(UUID extraContribId);

    Optional<ExtraContributionMember> findByExtraContribIdAndMemberId(UUID extraContribId, UUID memberId);
}
