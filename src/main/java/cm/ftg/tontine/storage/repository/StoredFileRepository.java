package cm.ftg.tontine.storage.repository;

import cm.ftg.tontine.storage.entity.StoredFile;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StoredFileRepository extends JpaRepository<StoredFile, UUID> {

    Optional<StoredFile> findByIdAndDeletedAtIsNull(UUID id);
}
