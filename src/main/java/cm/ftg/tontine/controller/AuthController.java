package cm.ftg.tontine.controller;

import cm.ftg.tontine.controller.doc.AuthControllerDoc;
import cm.ftg.tontine.service.AuthService;
import cm.ftg.tontine.service.LoginRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contrôleur d'authentification — endpoint de login.
 * Conformément à SKILL.md : injection par constructeur,
 * traçabilité Virtual Thread, documentation séparée.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController implements AuthControllerDoc {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Override
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        log.debug("POST /api/v1/auth/login — VirtualThread={}, thread={}",
            Thread.currentThread().isVirtual(), Thread.currentThread().getName());

        var result = authService.authentifier(request);
        return ResponseEntity.ok(new LoginResponse(
            result.accessToken(),
            "Bearer",
            result.userId(),
            result.email(),
            result.role()
        ));
    }
}

