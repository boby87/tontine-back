package cm.ftg.tontine.common.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(String resource, Object id) {
        super("RESOURCE_NOT_FOUND",
                "%s introuvable (id=%s)".formatted(resource, id),
                HttpStatus.NOT_FOUND);
    }
}
