package cm.ftg.tontine.auditor.certification.repository;

import cm.ftg.tontine.auditor.certification.entity.Certification;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CertificationRepository extends JpaRepository<Certification, UUID> {

    List<Certification> findByTontineIdOrderByIssuedAtDesc(UUID tontineId);
}
