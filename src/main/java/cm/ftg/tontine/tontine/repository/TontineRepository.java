package cm.ftg.tontine.tontine.repository;

import cm.ftg.tontine.tontine.entity.Tontine;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TontineRepository extends JpaRepository<Tontine, UUID> {

    @Query("""
            SELECT t FROM Tontine t
            WHERE LOWER(t.name) LIKE LOWER(CONCAT('%', :search, '%'))
               OR :search IS NULL
            """)
    Page<Tontine> search(@Param("search") String search, Pageable pageable);
}
