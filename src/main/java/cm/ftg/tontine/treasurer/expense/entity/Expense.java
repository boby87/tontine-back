package cm.ftg.tontine.treasurer.expense.entity;

import cm.ftg.tontine.treasurer.common.enums.PaymentMethod;
import cm.ftg.tontine.treasurer.expense.enums.ExpenseCategory;
import cm.ftg.tontine.treasurer.expense.enums.ExpenseStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "expenses", indexes = {
        @Index(name = "idx_expense_tontine", columnList = "tontine_id"),
        @Index(name = "idx_expense_status", columnList = "status"),
        @Index(name = "idx_expense_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
public class Expense {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tontine_id", nullable = false)
    private UUID tontineId;

    @Column(name = "cash_box_id", nullable = false)
    private UUID cashBoxId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ExpenseCategory category;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 1000)
    private String description;

    @Column(length = 160)
    private String vendor;

    @Column(name = "receipt_file_name", length = 255)
    private String receiptFileName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ExpenseStatus status = ExpenseStatus.DRAFT;

    @Column(name = "needs_validation", nullable = false)
    private boolean needsValidation = false;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal cap;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 30)
    private PaymentMethod paymentMethod;

    @Column(name = "created_by_full_name", nullable = false, length = 160)
    private String createdByFullName;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @Version
    private Long version;
}
