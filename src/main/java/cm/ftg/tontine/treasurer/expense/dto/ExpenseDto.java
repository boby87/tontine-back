package cm.ftg.tontine.treasurer.expense.dto;

import cm.ftg.tontine.treasurer.common.enums.PaymentMethod;
import cm.ftg.tontine.treasurer.expense.entity.Expense;
import cm.ftg.tontine.treasurer.expense.enums.ExpenseCategory;
import cm.ftg.tontine.treasurer.expense.enums.ExpenseStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ExpenseDto(
        UUID id,
        UUID tontineId,
        UUID cashBoxId,
        ExpenseCategory category,
        BigDecimal amount,
        String description,
        String vendor,
        String receiptFileName,
        ExpenseStatus status,
        boolean needsValidation,
        BigDecimal cap,
        Instant paidAt,
        PaymentMethod paymentMethod,
        String createdByFullName,
        Instant createdAt
) {

    public static ExpenseDto from(Expense e) {
        return new ExpenseDto(e.getId(), e.getTontineId(), e.getCashBoxId(), e.getCategory(),
                e.getAmount(), e.getDescription(), e.getVendor(), e.getReceiptFileName(),
                e.getStatus(), e.isNeedsValidation(), e.getCap(), e.getPaidAt(), e.getPaymentMethod(),
                e.getCreatedByFullName(), e.getCreatedAt());
    }
}
