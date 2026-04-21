package cm.ftg.tontine.controller;

import cm.ftg.tontine.controller.doc.DashboardControllerDoc;
import cm.ftg.tontine.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Contrôleur REST du dashboard de synthèse d'une tontine.
 * <p>
 * Sécurité :
 * TODO — Activer {@code @PreAuthorize("@tontineSecurity.isMemberOf(#tontineId)")} quand le bean sera disponible.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController implements DashboardControllerDoc {

    private final DashboardService dashboardService;
    private final DashboardMapper dashboardMapper;

    /**
     * GET /api/v1/dashboard/summary/{tontineId}
     * Retourne le résumé du dashboard pour la tontine spécifiée.
     */
    @Override
    @GetMapping("/summary/{tontineId}")
    // TODO @PreAuthorize("@tontineSecurity.isMemberOf(#tontineId)")
    public ResponseEntity<DashboardResponse> getSummary(@PathVariable Long tontineId) {
        log.debug("GET /api/v1/dashboard/summary/{} — VirtualThread={}, thread={}",
            tontineId, Thread.currentThread().isVirtual(), Thread.currentThread().getName());

        var summary = dashboardService.getDashboardSummary(tontineId);
        return ResponseEntity.ok(dashboardMapper.toResponse(summary));
    }
}

