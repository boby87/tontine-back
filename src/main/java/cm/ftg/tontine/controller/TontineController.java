package cm.ftg.tontine.controller;

import cm.ftg.tontine.controller.doc.TontineControllerDoc;
import cm.ftg.tontine.service.CreateTontineRequest;
import cm.ftg.tontine.service.TontineService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/tontines")
@RequiredArgsConstructor
public class TontineController implements TontineControllerDoc {

    private final TontineService tontineService;
    private final TontineMapper tontineMapper;

    @Override
    @PostMapping
    public ResponseEntity<TontineResponse> creer(@Valid @RequestBody CreateTontineRequest request) {
        log.debug("POST /api/v1/tontines — VirtualThread={}, thread={}",
            Thread.currentThread().isVirtual(), Thread.currentThread().getName());

        var result = tontineService.creerTontine(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(tontineMapper.toResponse(result));
    }

    @Override
    @GetMapping("/{id}")
    public ResponseEntity<TontineResponse> consulter(@PathVariable Long id) {
        log.debug("GET /api/v1/tontines/{} — VirtualThread={}, thread={}",
            id, Thread.currentThread().isVirtual(), Thread.currentThread().getName());

        var result = tontineService.findById(id);
        return ResponseEntity.ok(tontineMapper.toResponse(result));
    }
}
