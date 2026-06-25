package cm.ftg.tontine.common.exception;

import cm.ftg.tontine.common.dto.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApi(ApiException ex, HttpServletRequest req) {
        return ResponseEntity.status(ex.getStatus())
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiError.of(ex.getCode(), ex.getMessage(), ex.getStatus().value(), req.getRequestURI()));
    }

    private static final HttpStatus UNPROCESSABLE = HttpStatus.valueOf(422);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String, List<String>> details = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(fe ->
                details.computeIfAbsent(fe.getField(), k -> new java.util.ArrayList<>())
                        .add(fe.getDefaultMessage()));
        return ResponseEntity.status(UNPROCESSABLE)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiError.of("VALIDATION_FAILED", "Donnees invalides",
                        UNPROCESSABLE.value(), req.getRequestURI(), details));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraint(ConstraintViolationException ex, HttpServletRequest req) {
        return ResponseEntity.status(UNPROCESSABLE)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiError.of("VALIDATION_FAILED", ex.getMessage(),
                        UNPROCESSABLE.value(), req.getRequestURI()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentials(BadCredentialsException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiError.of("AUTH_INVALID_CREDENTIALS", "Identifiant ou mot de passe incorrect",
                        HttpStatus.UNAUTHORIZED.value(), req.getRequestURI()));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuth(AuthenticationException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiError.of("AUTH_REQUIRED", "Authentification requise",
                        HttpStatus.UNAUTHORIZED.value(), req.getRequestURI()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleForbidden(AccessDeniedException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiError.of("FORBIDDEN", "Acces refuse",
                        HttpStatus.FORBIDDEN.value(), req.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex, HttpServletRequest req) {
        log.error("Erreur non geree sur {}", req.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiError.of("INTERNAL_ERROR", "Une erreur interne est survenue",
                        HttpStatus.INTERNAL_SERVER_ERROR.value(), req.getRequestURI()));
    }
}
