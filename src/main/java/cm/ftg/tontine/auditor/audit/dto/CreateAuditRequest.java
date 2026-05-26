package cm.ftg.tontine.auditor.audit.dto;

import cm.ftg.tontine.auditor.audit.enums.AuditFinding;
import cm.ftg.tontine.auditor.audit.enums.AuditScope;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record CreateAuditRequest(
        @NotNull AuditScope scope,
        @NotNull LocalDate periodFrom,
        @NotNull LocalDate periodTo,
        @NotEmpty @Valid List<FindingInput> findings,
        @NotNull AuditFinding overallFinding,
        @Size(max = 4000) String observations
) {

    public record FindingInput(
            @NotBlank @Size(max = 160) String area,
            @NotNull AuditFinding finding,
            @NotBlank @Size(max = 2000) String description
    ) {
    }
}
