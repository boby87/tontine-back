package cm.ftg.tontine.notification.controller;

import cm.ftg.tontine.common.dto.ApiListResponse;
import cm.ftg.tontine.common.dto.ApiResponse;
import cm.ftg.tontine.notification.dto.NotificationDto;
import cm.ftg.tontine.notification.dto.ReadAllResult;
import cm.ftg.tontine.notification.service.NotificationService;
import cm.ftg.tontine.security.AuthenticatedUser;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    @GetMapping
    public ApiListResponse<NotificationDto> list(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader(value = "X-Tontine-Id", required = false) UUID tontineId,
            @RequestParam(required = false) Boolean unreadOnly,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), Math.min(pageSize, 100));
        Page<NotificationDto> result = service.list(user.id(), tontineId, unreadOnly, pageable)
                .map(NotificationDto::from);
        return ApiListResponse.of(result);
    }

    @GetMapping("/unread-count")
    public ApiResponse<Long> unreadCount(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.ok(service.countUnread(user.id()));
    }

    @PostMapping("/{id}/read")
    public ApiResponse<NotificationDto> markRead(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id) {
        return ApiResponse.ok(NotificationDto.from(service.markRead(id, user.id())));
    }

    @PostMapping("/read-all")
    public ApiResponse<ReadAllResult> markAllRead(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.ok(new ReadAllResult(service.markAllRead(user.id())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id) {
        service.delete(id, user.id());
        return ResponseEntity.noContent().build();
    }
}
