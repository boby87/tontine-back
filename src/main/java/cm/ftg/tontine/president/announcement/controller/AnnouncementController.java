package cm.ftg.tontine.president.announcement.controller;

import cm.ftg.tontine.common.dto.ApiResponse;
import cm.ftg.tontine.president.announcement.dto.AnnouncementDto;
import cm.ftg.tontine.president.announcement.dto.CreateAnnouncementRequest;
import cm.ftg.tontine.president.announcement.service.AnnouncementService;
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
@RequestMapping("/president/announcements")
public class AnnouncementController {

    private final AnnouncementService service;
    private final TontineIdResolver tontineIdResolver;

    public AnnouncementController(AnnouncementService service, TontineIdResolver tontineIdResolver) {
        this.service = service;
        this.tontineIdResolver = tontineIdResolver;
    }

    @GetMapping
    public ApiResponse<List<AnnouncementDto>> list(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.list(t, user.id()));
    }

    @PostMapping
    public ApiResponse<AnnouncementDto> publish(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId,
            @Valid @RequestBody CreateAnnouncementRequest req) {
        UUID t = tontineIdResolver.resolve(user.id(), tontineId);
        return ApiResponse.ok(service.publish(t, user.id(), req), "Annonce publiee");
    }
}
