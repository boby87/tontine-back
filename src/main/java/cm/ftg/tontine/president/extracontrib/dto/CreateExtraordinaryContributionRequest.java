package cm.ftg.tontine.president.extracontrib.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateExtraordinaryContributionRequest(
        @NotBlank @Size(max = 500) String motive,
        UUID beneficiaryMemberId,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amountPerMember,
        @NotNull LocalDate dueDate,
        boolean exemptBeneficiary
) {
}
