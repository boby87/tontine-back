package cm.ftg.tontine.secretary.archive.repository;

import cm.ftg.tontine.secretary.archive.entity.ArchiveDocument;
import cm.ftg.tontine.secretary.archive.enums.ArchiveDocumentType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ArchiveDocumentRepository extends JpaRepository<ArchiveDocument, UUID> {

    List<ArchiveDocument> findByTontineIdOrderByUploadedAtDesc(UUID tontineId);

    List<ArchiveDocument> findByTontineIdAndTypeOrderByUploadedAtDesc(UUID tontineId, ArchiveDocumentType type);

    List<ArchiveDocument> findByTontineIdAndCycleNumberOrderByUploadedAtDesc(UUID tontineId, Integer cycleNumber);

    List<ArchiveDocument> findByTontineIdAndTypeAndCycleNumberOrderByUploadedAtDesc(
            UUID tontineId, ArchiveDocumentType type, Integer cycleNumber);

    long countByTontineId(UUID tontineId);
}
