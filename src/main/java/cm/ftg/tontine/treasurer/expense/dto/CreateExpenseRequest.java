package cm.ftg.tontine.treasurer.expense.dto;

import cm.ftg.tontine.treasurer.expense.enums.ExpenseCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record CreateExpenseRequest(
        @NotNull ExpenseCategory category,
        @NotNull @DecimalMin(value = "0.01") @Digits(integer = 17, fraction = 2) BigDecimal amount,
        @NotBlank @Size(max = 1000) String description,
        @Size(max = 160) String vendor,
        @Size(max = 255) String receiptFileName,
        @NotNull UUID cashBoxId
) {
}
