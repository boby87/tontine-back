package cm.ftg.tontine.storage.controller;

import cm.ftg.tontine.common.dto.ApiResponse;
import cm.ftg.tontine.security.AuthenticatedUser;
import cm.ftg.tontine.storage.dto.StoredFileDto;
import cm.ftg.tontine.storage.entity.StoredFile;
import cm.ftg.tontine.storage.enums.StorageCategory;
import cm.ftg.tontine.storage.service.StoredFileService;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/files")
@PreAuthorize("isAuthenticated()")
public class StoredFileController {

    private final StoredFileService service;

    public StoredFileController(StoredFileService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<StoredFileDto> upload(@RequestPart("file") MultipartFile file,
                                             @RequestParam("category") StorageCategory category,
                                             @RequestParam(value = "tontineId", required = false) UUID tontineId,
                                             @AuthenticationPrincipal AuthenticatedUser principal) {
        StoredFile saved = service.upload(file, tontineId, principal.id(), category);
        return ApiResponse.ok(StoredFileDto.from(saved));
    }

    @GetMapping("/{id}")
    public ApiResponse<StoredFileDto> metadata(@PathVariable UUID id,
                                               @AuthenticationPrincipal AuthenticatedUser principal) {
        return ApiResponse.ok(StoredFileDto.from(service.getMetadata(id, principal.id())));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable UUID id,
                                             @AuthenticationPrincipal AuthenticatedUser principal) {
        StoredFileService.DownloadResult res = service.download(id, principal.id());
        StoredFile meta = res.metadata();
        String encodedName = URLEncoder.encode(meta.getOriginalName(), StandardCharsets.UTF_8)
                .replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + encodedName)
                .header(HttpHeaders.CONTENT_TYPE, meta.getContentType())
                .contentLength(meta.getSizeBytes())
                .body(res.resource());
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id,
                                    @AuthenticationPrincipal AuthenticatedUser principal) {
        service.delete(id, principal.id());
        return ApiResponse.ok(null);
    }
}
