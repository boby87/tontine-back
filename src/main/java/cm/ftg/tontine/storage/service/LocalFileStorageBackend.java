package cm.ftg.tontine.storage.service;

import cm.ftg.tontine.storage.config.StorageProperties;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.storage.backend", havingValue = "local", matchIfMissing = true)
public class LocalFileStorageBackend implements FileStorageBackend {

    private static final Logger log = LoggerFactory.getLogger(LocalFileStorageBackend.class);

    private final Path root;

    public LocalFileStorageBackend(StorageProperties properties) throws IOException {
        this.root = Path.of(properties.getLocal().getRoot()).toAbsolutePath().normalize();
        Files.createDirectories(this.root);
        log.info("LocalFileStorageBackend initialise sur {}", this.root);
    }

    @Override
    public String save(InputStream content, String suggestedKey, String contentType) throws IOException {
        Path target = resolveSafe(suggestedKey);
        Files.createDirectories(target.getParent());
        Files.copy(content, target, StandardCopyOption.REPLACE_EXISTING);
        return suggestedKey;
    }

    @Override
    public Resource load(String storageKey) {
        Path target = resolveSafe(storageKey);
        if (!Files.exists(target)) {
            throw new IllegalStateException("Fichier introuvable sur disque");
        }
        return new FileSystemResource(target);
    }

    @Override
    public void delete(String storageKey) {
        Path target = resolveSafe(storageKey);
        try {
            Files.deleteIfExists(target);
        } catch (NoSuchFileException ignored) {
            // idempotent
        } catch (IOException e) {
            log.warn("Echec suppression fichier {} : {}", storageKey, e.getMessage());
        }
    }

    @Override
    public boolean exists(String storageKey) {
        return Files.exists(resolveSafe(storageKey));
    }

    private Path resolveSafe(String storageKey) {
        Path target = root.resolve(storageKey).normalize();
        if (!target.startsWith(root)) {
            throw new IllegalArgumentException("Cle de stockage invalide");
        }
        return target;
    }
}
