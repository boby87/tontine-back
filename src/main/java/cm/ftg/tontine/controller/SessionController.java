package cm.ftg.tontine.controller;

import cm.ftg.tontine.controller.doc.SessionControllerDoc;
import cm.ftg.tontine.service.CreateSessionRequest;
import cm.ftg.tontine.service.SessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
public class SessionController implements SessionControllerDoc {

    private final SessionService sessionService;
    private final SessionMapper sessionMapper;

    @Override
    @PostMapping
    public ResponseEntity<SessionResponse> planifier(@Valid @RequestBody CreateSessionRequest request) {
        log.debug("POST /api/v1/sessions — VirtualThread={}, thread={}",
            Thread.currentThread().isVirtual(), Thread.currentThread().getName());

        var result = sessionService.planifierSession(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(sessionMapper.toResponse(result));
    }

    @Override
    @PutMapping("/{id}/ouvrir")
    public ResponseEntity<SessionResponse> ouvrir(@PathVariable Long id) {
        log.debug("PUT /api/v1/sessions/{}/ouvrir — VirtualThread={}, thread={}",
            id, Thread.currentThread().isVirtual(), Thread.currentThread().getName());

        var result = sessionService.ouvrirSession(id);
        return ResponseEntity.ok(sessionMapper.toResponse(result));
    }

    @Override
    @GetMapping("/{id}")
    public ResponseEntity<SessionResponse> consulter(@PathVariable Long id) {
        log.debug("GET /api/v1/sessions/{} — VirtualThread={}, thread={}",
            id, Thread.currentThread().isVirtual(), Thread.currentThread().getName());

        var result = sessionService.findById(id);
        return ResponseEntity.ok(sessionMapper.toResponse(result));
    }
}
