package cm.ftg.tontine.president.membership.repository;

import cm.ftg.tontine.president.membership.entity.MembershipFile;
import cm.ftg.tontine.president.membership.enums.MembershipFileKind;
import cm.ftg.tontine.president.membership.enums.MembershipFileStatus;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MembershipFileRepository extends JpaRepository<MembershipFile, UUID> {

    List<MembershipFile> findByTontineIdOrderBySubmittedAtDesc(UUID tontineId);

    List<MembershipFile> findByTontineIdAndKindOrderBySubmittedAtDesc(UUID tontineId, MembershipFileKind kind);

    List<MembershipFile> findByTontineIdAndStatusInOrderBySubmittedAtDesc(
            UUID tontineId, Collection<MembershipFileStatus> statuses);

    List<MembershipFile> findByTontineIdAndKindAndStatusInOrderBySubmittedAtDesc(
            UUID tontineId, MembershipFileKind kind, Collection<MembershipFileStatus> statuses);

    long countByTontineIdAndKindAndStatusIn(
            UUID tontineId, MembershipFileKind kind, Collection<MembershipFileStatus> statuses);
}
