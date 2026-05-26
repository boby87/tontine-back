package cm.ftg.tontine.treasurer.expense.repository;

import cm.ftg.tontine.treasurer.expense.entity.Expense;
import cm.ftg.tontine.treasurer.expense.enums.ExpenseStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, UUID> {

    List<Expense> findByTontineIdOrderByCreatedAtDesc(UUID tontineId);

    long countByTontineIdAndStatus(UUID tontineId, ExpenseStatus status);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e "
            + "WHERE e.tontineId = :tontineId AND e.status IN :statuses")
    BigDecimal sumAmountByTontineAndStatuses(@Param("tontineId") UUID tontineId,
                                              @Param("statuses") List<ExpenseStatus> statuses);
}
