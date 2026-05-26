package cm.ftg.tontine.president.validation.repository;

import cm.ftg.tontine.president.validation.entity.ValidationItem;
import cm.ftg.tontine.president.validation.enums.ValidationCategory;
import cm.ftg.tontine.president.validation.enums.ValidationStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ValidationItemRepository extends JpaRepository<ValidationItem, UUID> {

    List<ValidationItem> findByTontineIdAndStatusOrderBySubmittedAtDesc(UUID tontineId, ValidationStatus status);

    List<ValidationItem> findByTontineIdAndCategoryAndStatusOrderBySubmittedAtDesc(
            UUID tontineId, ValidationCategory category, ValidationStatus status);

    List<ValidationItem> findByTontineIdAndStatusAndAuditorOpinionStatusIsNullOrderBySubmittedAtDesc(
            UUID tontineId, ValidationStatus status);

    long countByTontineIdAndStatus(UUID tontineId, ValidationStatus status);
}
