package cm.ftg.tontine.censor.attendance.controller;

import cm.ftg.tontine.censor.attendance.dto.AttendanceModificationRequestDto;
import cm.ftg.tontine.censor.attendance.dto.DecideAttendanceModificationRequest;
import cm.ftg.tontine.censor.attendance.service.AttendanceModificationCensorService;
import cm.ftg.tontine.common.dto.ApiResponse;
import cm.ftg.tontine.president.common.TontineIdResolver;
import cm.ftg.tontine.security.AuthenticatedUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/censor/attendance-modifications")
public class AttendanceModificationCensorController {

    private final AttendanceModificationCensorService service;
    private final TontineIdResolver tontineIdResolver;

    public AttendanceModificationCensorController(AttendanceModificationCensorService service,
                                                  TontineIdResolver tontineIdResolver) {
        this.service = service;
        this.tontineIdResolver = tontineIdResolver;
    }

    @GetMapping
    public ApiResponse<List<AttendanceModificationRequestDto>> list(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.list(t, user.id()));
    }

    @PostMapping("/{id}/decide")
    public ApiResponse<AttendanceModificationRequestDto> decide(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId,
            @Valid @RequestBody DecideAttendanceModificationRequest req) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.decide(id, t, user.id(), req),
                "Decision enregistree");
    }
}
