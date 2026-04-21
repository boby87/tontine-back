package cm.ftg.tontine.controller;

import cm.ftg.tontine.controller.doc.TontineControllerDoc;
import cm.ftg.tontine.service.*;
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
    private final TontineInvitationService invitationService;
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

    @PostMapping("/{tontineId}/invite-link")
    public ResponseEntity<InviteLinkResult> genererLienInvitation(
            @PathVariable Long tontineId,
            @RequestParam String requestedByUserId,
            @RequestParam(required = false) Integer maxUses) {

        log.debug("POST /api/v1/tontines/{}/invite-link — VirtualThread={}",
            tontineId, Thread.currentThread().isVirtual());

        var request = new GenerateInviteLinkRequest(tontineId, requestedByUserId, maxUses);
        var result = invitationService.genererInvitation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PostMapping("/invitations/validate")
    public ResponseEntity<InvitationValidationResult> validerToken(@RequestParam String token) {

        log.debug("POST /api/v1/tontines/invitations/validate — VirtualThread={}",
            Thread.currentThread().isVirtual());

        var result = invitationService.validerToken(token);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{tontineId}/members/by-reference")
    public ResponseEntity<AddMemberResult> ajouterMembreParReference(
            @PathVariable Long tontineId,
            @Valid @RequestBody AddMemberByReferenceRequest request) {

        log.debug("POST /api/v1/tontines/{}/members/by-reference — VirtualThread={}",
            tontineId, Thread.currentThread().isVirtual());

        // Assurer la cohérence entre le path et le body
        var effectiveRequest = new AddMemberByReferenceRequest(tontineId, request.requestedByUserId(), request.reference());
        var result = tontineService.ajouterMembreParReference(effectiveRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
}
