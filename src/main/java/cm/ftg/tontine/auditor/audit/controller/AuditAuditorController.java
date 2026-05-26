package cm.ftg.tontine.auditor.audit.controller;

import cm.ftg.tontine.auditor.audit.dto.AuditDto;
import cm.ftg.tontine.auditor.audit.dto.CreateAuditRequest;
import cm.ftg.tontine.auditor.audit.service.AuditAuditorService;
import cm.ftg.tontine.common.dto.ApiResponse;
import cm.ftg.tontine.president.common.TontineIdResolver;
import cm.ftg.tontine.security.AuthenticatedUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auditor/audits")
public class AuditAuditorController {

    private final AuditAuditorService service;
    private final TontineIdResolver tontineIdResolver;

    public AuditAuditorController(AuditAuditorService service, TontineIdResolver tontineIdResolver) {
        this.service = service;
        this.tontineIdResolver = tontineIdResolver;
    }

    @GetMapping
    public ApiResponse<List<AuditDto>> list(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.list(t, user.id()));
    }

    @PostMapping
    public ApiResponse<AuditDto> create(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId,
            @Valid @RequestBody CreateAuditRequest req) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        String fullName = (user.email() != null) ? user.email() : user.id().toString();
        return ApiResponse.ok(service.create(t, user.id(), fullName, req), "Audit enregistre");
    }
}
