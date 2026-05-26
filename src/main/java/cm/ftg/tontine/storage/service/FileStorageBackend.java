package cm.ftg.tontine.storage.service;

import java.io.IOException;
import java.io.InputStream;
import org.springframework.core.io.Resource;

public interface FileStorageBackend {

    /**
     * Persiste le contenu et retourne la cle effective de stockage.
     * La cle peut differer de la cle suggeree si une collision est detectee.
     */
    String save(InputStream content, String suggestedKey, String contentType) throws IOException;

    /**
     * Charge le contenu en tant que Spring Resource lisible.
     */
    Resource load(String storageKey);

    /**
     * Supprime physiquement le fichier (operation idempotente).
     */
    void delete(String storageKey);

    boolean exists(String storageKey);
}
