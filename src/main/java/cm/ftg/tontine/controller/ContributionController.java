package cm.ftg.tontine.controller;

import cm.ftg.tontine.controller.doc.ContributionControllerDoc;
import cm.ftg.tontine.service.ContributionRequest;
import cm.ftg.tontine.service.ContributionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/contributions")
@RequiredArgsConstructor
public class ContributionController implements ContributionControllerDoc {

    private final ContributionService contributionService;
    private final ContributionMapper contributionMapper;

    @Override
    @PostMapping
    public ResponseEntity<ContributionResponse> enregistrer(@Valid @RequestBody ContributionRequest request) {
        log.debug("POST /api/v1/contributions — VirtualThread={}, thread={}",
            Thread.currentThread().isVirtual(), Thread.currentThread().getName());

        var result = contributionService.recordContribution(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(contributionMapper.toResponse(result));
    }

    @Override
    @GetMapping("/{id}")
    public ResponseEntity<ContributionResponse> consulter(@PathVariable Long id) {
        log.debug("GET /api/v1/contributions/{} — VirtualThread={}, thread={}",
            id, Thread.currentThread().isVirtual(), Thread.currentThread().getName());

        var result = contributionService.findById(id);
        return ResponseEntity.ok(contributionMapper.toResponse(result));
    }
}
