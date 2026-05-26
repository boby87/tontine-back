package cm.ftg.tontine.secretary.attendance.controller;

import cm.ftg.tontine.common.dto.ApiResponse;
import cm.ftg.tontine.president.common.TontineIdResolver;
import cm.ftg.tontine.president.session.dto.SessionAttendanceEntryDto;
import cm.ftg.tontine.secretary.attendance.dto.UpsertAttendanceRequest;
import cm.ftg.tontine.secretary.attendance.service.AttendanceSecretaryService;
import cm.ftg.tontine.security.AuthenticatedUser;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/secretary/sessions")
public class AttendanceSecretaryController {

    private final AttendanceSecretaryService service;
    private final TontineIdResolver tontineIdResolver;

    public AttendanceSecretaryController(AttendanceSecretaryService service,
                                         TontineIdResolver tontineIdResolver) {
        this.service = service;
        this.tontineIdResolver = tontineIdResolver;
    }

    @PostMapping("/{sessionId}/attendance/{memberId}")
    public ApiResponse<SessionAttendanceEntryDto> upsert(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID sessionId,
            @PathVariable UUID memberId,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId,
            @Valid @RequestBody UpsertAttendanceRequest req) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.upsert(sessionId, memberId, t, user.id(), req),
                "Presence enregistree");
    }

    @PostMapping("/{sessionId}/attendance/finalize")
    public ResponseEntity<Void> finalize(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID sessionId,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        service.finalize(sessionId, t, user.id());
        return ResponseEntity.noContent().build();
    }
}
