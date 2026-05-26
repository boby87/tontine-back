package cm.ftg.tontine.auditor.certification.dto;

import cm.ftg.tontine.auditor.certification.enums.CertificationDecision;
import cm.ftg.tontine.auditor.certification.enums.CertificationScope;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record IssueCertificationRequest(
        @NotNull CertificationScope scope,
        @NotBlank @Size(max = 80) String periodLabel,
        @NotNull CertificationDecision decision,
        @Size(max = 4000) String reserves,
        @NotBlank @Pattern(regexp = "^\\d{6}$") String otp
) {
}
