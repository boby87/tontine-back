package cm.ftg.tontine.auth.service;

import cm.ftg.tontine.audit.service.AuditService;
import cm.ftg.tontine.auth.dto.AuthSessionDto;
import cm.ftg.tontine.auth.dto.ForgotPasswordRequest;
import cm.ftg.tontine.auth.dto.IdentifierResponse;
import cm.ftg.tontine.auth.dto.LoginRequest;
import cm.ftg.tontine.auth.dto.LoginResult;
import cm.ftg.tontine.auth.dto.OtpVerifyRequest;
import cm.ftg.tontine.auth.dto.RefreshRequest;
import cm.ftg.tontine.auth.dto.RegisterRequest;
import cm.ftg.tontine.auth.dto.ResendOtpRequest;
import cm.ftg.tontine.auth.dto.ResetPasswordRequest;
import cm.ftg.tontine.auth.dto.TotpChallengeDto;
import cm.ftg.tontine.auth.dto.TotpConfirmRequest;
import cm.ftg.tontine.auth.dto.TotpSetupDto;
import cm.ftg.tontine.auth.dto.TotpVerifyLoginRequest;
import cm.ftg.tontine.auth.dto.TokensDto;
import cm.ftg.tontine.auth.dto.UserDto;
import cm.ftg.tontine.auth.entity.UserEntity;
import cm.ftg.tontine.auth.repository.UserRepository;
import cm.ftg.tontine.common.enums.MemberStatus;
import cm.ftg.tontine.common.enums.OtpPurpose;
import cm.ftg.tontine.common.enums.UserRole;
import cm.ftg.tontine.common.enums.UserStatus;
import cm.ftg.tontine.common.exception.ApiException;
import cm.ftg.tontine.member.entity.Member;
import cm.ftg.tontine.member.repository.MemberRepository;
import cm.ftg.tontine.security.JwtService;
import java.util.Base64;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final String TOTP_ISSUER = "TontineApp";

    private final UserRepository userRepository;
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final JwtService jwtService;
    private final TotpService totpService;
    private final AuditService auditService;

    public AuthService(UserRepository userRepository,
                       MemberRepository memberRepository,
                       PasswordEncoder passwordEncoder,
                       OtpService otpService,
                       JwtService jwtService,
                       TotpService totpService,
                       AuditService auditService) {
        this.userRepository = userRepository;
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
        this.otpService = otpService;
        this.jwtService = jwtService;
        this.totpService = totpService;
        this.auditService = auditService;
    }

    @Transactional
    public IdentifierResponse register(RegisterRequest req) {
        var byEmail = userRepository.findByEmail(req.email().toLowerCase().trim());
        if (byEmail.isPresent()) {
            UserEntity existing = byEmail.get();
            if (existing.getStatus() == UserStatus.PENDING_VERIFICATION) {
                otpService.resend(existing.getPhone(), OtpPurpose.REGISTRATION);
                return new IdentifierResponse(existing.getPhone());
            }
            throw new ApiException("AUTH_USER_EXISTS",
                    "Un compte existe deja avec cet email", HttpStatus.CONFLICT);
        }
        var byPhone = userRepository.findByPhone(req.phone().trim());
        if (byPhone.isPresent()) {
            UserEntity existing = byPhone.get();
            if (existing.getStatus() == UserStatus.PENDING_VERIFICATION) {
                otpService.resend(existing.getPhone(), OtpPurpose.REGISTRATION);
                return new IdentifierResponse(existing.getPhone());
            }
            throw new ApiException("AUTH_USER_EXISTS",
                    "Un compte existe deja avec ce telephone", HttpStatus.CONFLICT);
        }
        UserEntity u = new UserEntity();
        u.setFirstName(req.firstName().trim());
        u.setLastName(req.lastName().trim());
        u.setEmail(req.email().toLowerCase().trim());
        u.setPhone(req.phone().trim());
        u.setPasswordHash(passwordEncoder.encode(req.password()));
        u.setRoles(EnumSet.of(UserRole.MEMBER));
        u.setStatus(UserStatus.PENDING_VERIFICATION);
        UserEntity saved = userRepository.save(u);
        otpService.issue(saved.getPhone(), OtpPurpose.REGISTRATION);
        auditService.record(saved.getId(), "AUTH_REGISTER", "User", saved.getId().toString(),
                null, "{\"email\":\"" + saved.getEmail() + "\"}");
        return new IdentifierResponse(saved.getPhone());
    }

    @Transactional
    public AuthSessionDto verifyOtp(OtpVerifyRequest req) {
        UserEntity user = userRepository.findByIdentifier(req.identifier())
                .orElseThrow(() -> new ApiException("AUTH_OTP_INVALID",
                        "Identifiant inconnu", HttpStatus.UNAUTHORIZED));
        String otpIdentifier = resolveOtpIdentifier(user);
        otpService.verifyAndConsume(otpIdentifier, req.code(), OtpPurpose.REGISTRATION);
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
        auditService.record(user.getId(), "AUTH_OTP_VERIFIED", "User", user.getId().toString(),
                null, null);
        return buildSession(user);
    }

    @Transactional
    public LoginResult login(LoginRequest req) {
        UserEntity user = userRepository.findByIdentifier(req.identifier())
                .orElseThrow(() -> new ApiException("AUTH_INVALID_CREDENTIALS",
                        "Identifiant ou mot de passe incorrect", HttpStatus.UNAUTHORIZED));
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new ApiException("AUTH_INVALID_CREDENTIALS",
                    "Identifiant ou mot de passe incorrect", HttpStatus.UNAUTHORIZED);
        }
        if (!user.isActive()) {
            if (user.getStatus() == UserStatus.PENDING_VERIFICATION) {
                String identifier = user.getPhone() != null ? user.getPhone() : user.getEmail();
                otpService.resend(identifier, OtpPurpose.REGISTRATION);
                throw new ApiException("AUTH_ACCOUNT_UNVERIFIED",
                        "Compte non verifie. Un nouveau code OTP a ete envoye.", HttpStatus.FORBIDDEN);
            }
            throw new ApiException("AUTH_ACCOUNT_DISABLED", "Compte desactive", HttpStatus.FORBIDDEN);
        }
        if (user.isTotpEnabled()) {
            String pendingToken = jwtService.generateTotpPendingToken(user);
            try {
                auditService.record(user.getId(), "AUTH_TOTP_CHALLENGE", "User",
                        user.getId().toString(), null, null);
            } catch (Exception ignored) {
            }
            return new LoginResult.TotpRequired(TotpChallengeDto.of(pendingToken));
        }
        auditService.record(user.getId(), "AUTH_LOGIN", "User", user.getId().toString(), null, null);
        return new LoginResult.Authenticated(buildSession(user));
    }

    @Transactional
    public AuthSessionDto verifyTotpLogin(TotpVerifyLoginRequest req) {
        JwtService.ParsedToken parsed;
        try {
            parsed = jwtService.parse(req.totpPendingToken());
        } catch (JwtService.InvalidJwtException ex) {
            throw new ApiException("AUTH_TOTP_TOKEN_EXPIRED",
                    "Session TOTP expiree, veuillez vous reconnecter", HttpStatus.UNAUTHORIZED);
        }
        if (parsed.type() != JwtService.TokenType.TOTP_PENDING) {
            throw new ApiException("AUTH_TOKEN_INVALID",
                    "Type de token incorrect", HttpStatus.UNAUTHORIZED);
        }
        UserEntity user = userRepository.findById(parsed.userId())
                .orElseThrow(() -> new ApiException("AUTH_REQUIRED",
                        "Utilisateur introuvable", HttpStatus.UNAUTHORIZED));
        if (!user.isTotpEnabled() || user.getTotpSecret() == null) {
            throw new ApiException("TOTP_NOT_CONFIGURED",
                    "La 2FA TOTP n'est pas activee pour ce compte", HttpStatus.CONFLICT);
        }
        if (!totpService.verify(user.getTotpSecret(), req.code())) {
            throw new ApiException("TOTP_CODE_INVALID",
                    "Code TOTP invalide", HttpStatus.UNAUTHORIZED);
        }
        auditService.record(user.getId(), "AUTH_TOTP_VERIFIED", "User",
                user.getId().toString(), null, null);
        return buildSession(user);
    }

    @Transactional
    public TotpSetupDto setupTotp(UUID userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("AUTH_REQUIRED",
                        "Utilisateur introuvable", HttpStatus.UNAUTHORIZED));
        if (user.isTotpEnabled()) {
            throw new ApiException("TOTP_ALREADY_ENABLED",
                    "La 2FA TOTP est deja activee. Desactivez-la d'abord.", HttpStatus.CONFLICT);
        }
        String secret = totpService.generateSecret();
        user.setTotpSecret(secret);
        userRepository.save(user);
        String uri = totpService.buildOtpauthUri(user.getEmail(), TOTP_ISSUER, secret);
        byte[] qrBytes = totpService.generateQrCodePng(uri);
        String qrBase64 = Base64.getEncoder().encodeToString(qrBytes);
        return new TotpSetupDto(secret, uri, qrBase64);
    }

    @Transactional
    public void confirmTotp(UUID userId, TotpConfirmRequest req) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("AUTH_REQUIRED",
                        "Utilisateur introuvable", HttpStatus.UNAUTHORIZED));
        if (user.getTotpSecret() == null) {
            throw new ApiException("TOTP_SETUP_REQUIRED",
                    "Initialisez d'abord la 2FA via POST /auth/totp/setup", HttpStatus.CONFLICT);
        }
        if (!totpService.verify(user.getTotpSecret(), req.code())) {
            throw new ApiException("TOTP_CODE_INVALID",
                    "Code TOTP invalide. Verifiez l'heure de votre appareil.", HttpStatus.UNAUTHORIZED);
        }
        user.setTotpEnabled(true);
        userRepository.save(user);
        try {
            auditService.record(userId, "TOTP_ENABLED", "User", userId.toString(), null, null);
        } catch (Exception ignored) {
        }
    }

    @Transactional
    public void disableTotp(UUID userId, String password) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("AUTH_REQUIRED",
                        "Utilisateur introuvable", HttpStatus.UNAUTHORIZED));
        if (!user.isTotpEnabled()) {
            throw new ApiException("TOTP_NOT_ENABLED",
                    "La 2FA TOTP n'est pas activee", HttpStatus.CONFLICT);
        }
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new ApiException("AUTH_INVALID_CREDENTIALS",
                    "Mot de passe incorrect", HttpStatus.UNAUTHORIZED);
        }
        user.setTotpEnabled(false);
        user.setTotpSecret(null);
        userRepository.save(user);
        try {
            auditService.record(userId, "TOTP_DISABLED", "User", userId.toString(), null, null);
        } catch (Exception ignored) {
        }
    }

    @Transactional
    public IdentifierResponse resendOtp(ResendOtpRequest req) {
        UserEntity user = userRepository.findByIdentifier(req.identifier())
                .orElseThrow(() -> new ApiException("AUTH_USER_NOT_FOUND",
                        "Utilisateur introuvable", HttpStatus.NOT_FOUND));
        if (user.getStatus() != UserStatus.PENDING_VERIFICATION) {
            throw new ApiException("AUTH_ALREADY_VERIFIED",
                    "Ce compte est deja verifie", HttpStatus.CONFLICT);
        }
        String otpIdentifier = resolveOtpIdentifier(user);
        otpService.resend(otpIdentifier, OtpPurpose.REGISTRATION);
        return new IdentifierResponse(otpIdentifier);
    }

    @Transactional
    public IdentifierResponse forgotPassword(ForgotPasswordRequest req) {
        userRepository.findByIdentifier(req.identifier()).ifPresent(u ->
                otpService.issue(req.identifier(), OtpPurpose.PASSWORD_RESET));
        return new IdentifierResponse(req.identifier());
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest req) {
        UserEntity user = userRepository.findByIdentifier(req.identifier())
                .orElseThrow(() -> new ApiException("AUTH_OTP_INVALID",
                        "OTP invalide", HttpStatus.UNAUTHORIZED));
        otpService.verifyAndConsume(req.identifier(), req.code(), OtpPurpose.PASSWORD_RESET);
        user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        userRepository.save(user);
        auditService.record(user.getId(), "AUTH_PASSWORD_RESET", "User", user.getId().toString(),
                null, null);
    }

    @Transactional
    public AuthSessionDto refresh(RefreshRequest req) {
        JwtService.ParsedToken parsed;
        try {
            parsed = jwtService.parse(req.refreshToken());
        } catch (JwtService.InvalidJwtException ex) {
            throw new ApiException("AUTH_TOKEN_EXPIRED",
                    "Refresh token invalide ou expire", HttpStatus.UNAUTHORIZED);
        }
        if (parsed.type() != JwtService.TokenType.REFRESH) {
            throw new ApiException("AUTH_TOKEN_EXPIRED",
                    "Type de token incorrect", HttpStatus.UNAUTHORIZED);
        }
        UserEntity user = userRepository.findById(parsed.userId())
                .orElseThrow(() -> new ApiException("AUTH_TOKEN_EXPIRED",
                        "Utilisateur introuvable", HttpStatus.UNAUTHORIZED));
        return buildSession(user);
    }

    @Transactional(readOnly = true)
    public UserDto me(UUID userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("AUTH_REQUIRED",
                        "Session invalide", HttpStatus.UNAUTHORIZED));
        return UserDto.from(user, aggregateRoles(user));
    }

    public void logout(UUID userId) {
        if (userId != null) {
            auditService.record(userId, "AUTH_LOGOUT", "User", userId.toString(), null, null);
        }
    }

    private AuthSessionDto buildSession(UserEntity user) {
        TokensDto tokens = new TokensDto(
                jwtService.generateAccessToken(user),
                jwtService.generateRefreshToken(user),
                jwtService.accessTokenTtlSeconds());
        return new AuthSessionDto(
                UserDto.from(user, aggregateRoles(user)),
                tokens,
                user.getActiveTontineId());
    }

    private Set<UserRole> aggregateRoles(UserEntity user) {
        EnumSet<UserRole> roles = EnumSet.copyOf(user.getRoles());
        for (Member m : memberRepository.findByUserId(user.getId())) {
            if (m.getStatus() == MemberStatus.ACTIVE) {
                roles.addAll(m.getRoles());
            }
        }
        return roles;
    }

    private String resolveOtpIdentifier(UserEntity user) {
        return user.getPhone() != null ? user.getPhone() : user.getEmail();
    }
}
