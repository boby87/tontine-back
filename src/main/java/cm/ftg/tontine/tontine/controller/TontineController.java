package cm.ftg.tontine.tontine.controller;

import cm.ftg.tontine.common.dto.ApiListResponse;
import cm.ftg.tontine.common.dto.ApiResponse;
import cm.ftg.tontine.security.AuthenticatedUser;
import cm.ftg.tontine.tontine.dto.CreateTontineRequest;
import cm.ftg.tontine.tontine.dto.CycleDto;
import cm.ftg.tontine.tontine.dto.TontineDto;
import cm.ftg.tontine.tontine.dto.UpdateTontineRequest;
import cm.ftg.tontine.tontine.service.TontineService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tontines")
public class TontineController {

    private final TontineService tontineService;

    public TontineController(TontineService tontineService) {
        this.tontineService = tontineService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiListResponse<TontineDto> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String search) {
        Sort sort = "asc".equalsIgnoreCase(sortDir) ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), pageSize, sort);
        Page<TontineDto> result = tontineService.list(search, pageable);
        return ApiListResponse.of(result);
    }

    @GetMapping("/mine")
    public ApiResponse<List<TontineDto>> mine(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.ok(tontineService.listMine(user.id()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TontineDto>> create(@AuthenticationPrincipal AuthenticatedUser user,
                                                          @Valid @RequestBody CreateTontineRequest req) {
        TontineDto created = tontineService.create(user.id(), req);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(created, "Tontine creee"));
    }

    @GetMapping("/{id}")
    public ApiResponse<TontineDto> getById(@AuthenticationPrincipal AuthenticatedUser user,
                                           @PathVariable UUID id) {
        return ApiResponse.ok(tontineService.findById(id, user.id(), user.roles()));
    }

    @PatchMapping("/{id}")
    public ApiResponse<TontineDto> update(@AuthenticationPrincipal AuthenticatedUser user,
                                          @PathVariable UUID id,
                                          @Valid @RequestBody UpdateTontineRequest req) {
        return ApiResponse.ok(tontineService.update(id, req, user.id()));
    }

    @GetMapping("/{id}/cycles")
    public ApiResponse<List<CycleDto>> cycles(@AuthenticationPrincipal AuthenticatedUser user,
                                              @PathVariable UUID id) {
        return ApiResponse.ok(tontineService.listCycles(id, user.id(), user.roles()));
    }
}
