package cm.ftg.tontine.auth.controller;

import cm.ftg.tontine.auth.dto.AuthSessionDto;
import cm.ftg.tontine.auth.dto.ForgotPasswordRequest;
import cm.ftg.tontine.auth.dto.IdentifierResponse;
import cm.ftg.tontine.auth.dto.LoginRequest;
import cm.ftg.tontine.auth.dto.OtpVerifyRequest;
import cm.ftg.tontine.auth.dto.RefreshRequest;
import cm.ftg.tontine.auth.dto.RegisterRequest;
import cm.ftg.tontine.auth.dto.ResetPasswordRequest;
import cm.ftg.tontine.auth.dto.UserDto;
import cm.ftg.tontine.auth.service.AuthService;
import cm.ftg.tontine.common.dto.ApiResponse;
import cm.ftg.tontine.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<IdentifierResponse>> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(authService.register(req), "Verifiez votre OTP pour activer le compte"));
    }

    @PostMapping("/verify-otp")
    public ApiResponse<AuthSessionDto> verifyOtp(@Valid @RequestBody OtpVerifyRequest req) {
        return ApiResponse.ok(authService.verifyOtp(req));
    }

    @PostMapping("/login")
    public ApiResponse<AuthSessionDto> login(@Valid @RequestBody LoginRequest req) {
        return ApiResponse.ok(authService.login(req));
    }

    @PostMapping("/forgot-password")
    public ApiResponse<IdentifierResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        return ApiResponse.ok(authService.forgotPassword(req),
                "Si le compte existe, un OTP a ete envoye");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        authService.resetPassword(req);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthSessionDto> refresh(@Valid @RequestBody RefreshRequest req) {
        return ApiResponse.ok(authService.refresh(req));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal AuthenticatedUser user) {
        authService.logout(user != null ? user.id() : null);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ApiResponse<UserDto> me(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.ok(authService.me(user.id()));
    }
}
