package cm.ftg.tontine.president.report.repository;

import cm.ftg.tontine.president.report.entity.ReportEntry;
import cm.ftg.tontine.president.report.enums.ReportCategory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportEntryRepository extends JpaRepository<ReportEntry, UUID> {

    List<ReportEntry> findByTontineIdOrderByGeneratedAtDesc(UUID tontineId);

    List<ReportEntry> findByTontineIdAndCategoryOrderByGeneratedAtDesc(UUID tontineId, ReportCategory category);
}
