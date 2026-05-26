package cm.ftg.tontine.storage.service;

import cm.ftg.tontine.audit.service.AuditService;
import cm.ftg.tontine.common.exception.ApiException;
import cm.ftg.tontine.common.exception.ResourceNotFoundException;
import cm.ftg.tontine.member.repository.MemberRepository;
import cm.ftg.tontine.storage.config.StorageProperties;
import cm.ftg.tontine.storage.entity.StoredFile;
import cm.ftg.tontine.storage.enums.StorageCategory;
import cm.ftg.tontine.storage.repository.StoredFileRepository;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class StoredFileService {

    private static final Logger log = LoggerFactory.getLogger(StoredFileService.class);
    private static final DateTimeFormatter DATE_PATH = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private static final Set<String> IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/heic", "image/heif");
    private static final Set<String> DOC_TYPES = Set.of(
            "application/pdf",
            "image/jpeg", "image/png", "image/webp",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

    private final StoredFileRepository repository;
    private final FileStorageBackend backend;
    private final StorageProperties properties;
    private final MemberRepository memberRepository;
    private final AuditService auditService;

    public StoredFileService(StoredFileRepository repository,
                             FileStorageBackend backend,
                             StorageProperties properties,
                             MemberRepository memberRepository,
                             AuditService auditService) {
        this.repository = repository;
        this.backend = backend;
        this.properties = properties;
        this.memberRepository = memberRepository;
        this.auditService = auditService;
    }

    @Transactional
    public StoredFile upload(MultipartFile file, UUID tontineId, UUID ownerUserId, StorageCategory category) {
        if (file == null || file.isEmpty()) {
            throw new ApiException("FILE_EMPTY", "Fichier manquant ou vide", HttpStatus.valueOf(422));
        }
        if (file.getSize() > properties.getMaxFileSizeBytes()) {
            throw new ApiException("FILE_TOO_LARGE",
                    "Taille maximale autorisee : " + properties.getMaxFileSizeBytes() + " octets",
                    HttpStatus.valueOf(422));
        }
        String contentType = file.getContentType() == null ? "application/octet-stream" : file.getContentType();
        validateMimeForCategory(category, contentType);
        ensureMembershipIfTontine(tontineId, ownerUserId);

        String safeName = sanitize(file.getOriginalFilename());
        String suggestedKey = category.name() + "/" + LocalDate.now().format(DATE_PATH)
                + "/" + UUID.randomUUID() + "-" + safeName;

        MessageDigest digest = newSha256();
        String finalKey;
        try (InputStream raw = file.getInputStream();
             DigestInputStream dis = new DigestInputStream(raw, digest)) {
            finalKey = backend.save(dis, suggestedKey, contentType);
        } catch (IOException e) {
            log.warn("Echec ecriture fichier ownerUser={} category={} : {}",
                    ownerUserId, category, e.getMessage());
            throw new ApiException("FILE_WRITE_FAILED",
                    "Impossible d'enregistrer le fichier", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        StoredFile entity = new StoredFile();
        entity.setTontineId(tontineId);
        entity.setOwnerUserId(ownerUserId);
        entity.setCategory(category);
        entity.setOriginalName(safeName);
        entity.setContentType(contentType);
        entity.setSizeBytes(file.getSize());
        entity.setStorageKey(finalKey);
        entity.setSha256(HexFormat.of().formatHex(digest.digest()));
        StoredFile saved = repository.save(entity);

        auditService.record(ownerUserId, "FILE_UPLOAD", "StoredFile", saved.getId().toString(),
                tontineId,
                "{\"category\":\"" + category.name() + "\",\"size\":" + saved.getSizeBytes() + "}");
        return saved;
    }

    @Transactional(readOnly = true)
    public StoredFile getMetadata(UUID fileId, UUID requesterUserId) {
        StoredFile f = loadAlive(fileId);
        ensureCanRead(f, requesterUserId);
        return f;
    }

    @Transactional(readOnly = true)
    public DownloadResult download(UUID fileId, UUID requesterUserId) {
        StoredFile f = loadAlive(fileId);
        ensureCanRead(f, requesterUserId);
        Resource resource = backend.load(f.getStorageKey());
        return new DownloadResult(f, resource);
    }

    @Transactional
    public void delete(UUID fileId, UUID requesterUserId) {
        StoredFile f = loadAlive(fileId);
        if (!f.getOwnerUserId().equals(requesterUserId)) {
            throw new ApiException("FORBIDDEN",
                    "Seul le proprietaire peut supprimer ce fichier", HttpStatus.FORBIDDEN);
        }
        f.setDeletedAt(Instant.now());
        repository.save(f);
        backend.delete(f.getStorageKey());
        auditService.record(requesterUserId, "FILE_DELETE", "StoredFile", fileId.toString(),
                f.getTontineId(), null);
    }

    private StoredFile loadAlive(UUID fileId) {
        return repository.findByIdAndDeletedAtIsNull(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("StoredFile", fileId));
    }

    private void ensureCanRead(StoredFile f, UUID requesterUserId) {
        if (f.getOwnerUserId().equals(requesterUserId)) {
            return;
        }
        if (f.getTontineId() != null
                && memberRepository.findByUserIdAndTontineId(requesterUserId, f.getTontineId()).isPresent()) {
            return;
        }
        throw new ApiException("FORBIDDEN",
                "Acces refuse a ce fichier", HttpStatus.FORBIDDEN);
    }

    private void ensureMembershipIfTontine(UUID tontineId, UUID userId) {
        if (tontineId == null) {
            return;
        }
        if (memberRepository.findByUserIdAndTontineId(userId, tontineId).isEmpty()) {
            throw new ApiException("FORBIDDEN",
                    "Utilisateur non membre de la tontine", HttpStatus.FORBIDDEN);
        }
    }

    private void validateMimeForCategory(StorageCategory category, String contentType) {
        Set<String> allowed = switch (category) {
            case KYC_PHOTO -> IMAGE_TYPES;
            case SANCTION_JUSTIFICATION, ABSENCE_JUSTIFICATION,
                 SESSION_PV, CONTRIBUTION_PROOF, MOBILE_MONEY_PROOF, OTHER -> DOC_TYPES;
        };
        if (!allowed.contains(contentType)) {
            throw new ApiException("FILE_TYPE_NOT_ALLOWED",
                    "Type de fichier non autorise pour cette categorie : " + contentType,
                    HttpStatus.valueOf(422));
        }
    }

    private String sanitize(String name) {
        if (name == null || name.isBlank()) {
            return "file";
        }
        String base = name.replaceAll("[\\\\/]+", "_");
        base = base.replaceAll("[^A-Za-z0-9._-]", "_");
        if (base.length() > 120) {
            base = base.substring(base.length() - 120);
        }
        return base;
    }

    private MessageDigest newSha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponible", e);
        }
    }

    public record DownloadResult(StoredFile metadata, Resource resource) {}
}
