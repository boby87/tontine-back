package cm.ftg.tontine.secretary.convocation.repository;

import cm.ftg.tontine.secretary.convocation.entity.Convocation;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConvocationRepository extends JpaRepository<Convocation, UUID> {

    List<Convocation> findByTontineIdOrderByCreatedAtDesc(UUID tontineId);
}
