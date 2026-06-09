package cm.ftg.tontine.auth.service;

import cm.ftg.tontine.audit.service.AuditService;
import cm.ftg.tontine.auth.dto.AuthSessionDto;
import cm.ftg.tontine.auth.dto.ForgotPasswordRequest;
import cm.ftg.tontine.auth.dto.IdentifierResponse;
import cm.ftg.tontine.auth.dto.LoginRequest;
import cm.ftg.tontine.auth.dto.OtpVerifyRequest;
import cm.ftg.tontine.auth.dto.RefreshRequest;
import cm.ftg.tontine.auth.dto.RegisterRequest;
import cm.ftg.tontine.auth.dto.ResetPasswordRequest;
import cm.ftg.tontine.auth.dto.TokensDto;
import cm.ftg.tontine.auth.dto.UserDto;
import cm.ftg.tontine.auth.entity.UserEntity;
import cm.ftg.tontine.auth.repository.UserRepository;
import cm.ftg.tontine.common.enums.MemberStatus;
import cm.ftg.tontine.common.enums.OtpPurpose;
import cm.ftg.tontine.common.enums.UserRole;
import cm.ftg.tontine.common.exception.ApiException;
import cm.ftg.tontine.member.entity.Member;
import cm.ftg.tontine.member.repository.MemberRepository;
import cm.ftg.tontine.security.JwtService;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final JwtService jwtService;
    private final AuditService auditService;

    public AuthService(UserRepository userRepository,
                       MemberRepository memberRepository,
                       PasswordEncoder passwordEncoder,
                       OtpService otpService,
                       JwtService jwtService,
                       AuditService auditService) {
        this.userRepository = userRepository;
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
        this.otpService = otpService;
        this.jwtService = jwtService;
        this.auditService = auditService;
    }

    @Transactional
    public IdentifierResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new ApiException("AUTH_USER_EXISTS",
                    "Un compte existe deja avec cet email", HttpStatus.CONFLICT);
        }
        if (userRepository.existsByPhone(req.phone())) {
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
        u.setActive(true);
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
        otpService.verifyAndConsume(req.identifier(), req.code(), OtpPurpose.REGISTRATION);
        if (isEmail(req.identifier())) {
            user.setEmailVerified(true);
        } else {
            user.setPhoneVerified(true);
        }
        userRepository.save(user);
        auditService.record(user.getId(), "AUTH_OTP_VERIFIED", "User", user.getId().toString(),
                null, null);
        return buildSession(user);
    }

    @Transactional
    public AuthSessionDto login(LoginRequest req) {
        UserEntity user = userRepository.findByIdentifier(req.identifier())
                .orElseThrow(() -> new ApiException("AUTH_INVALID_CREDENTIALS",
                        "Identifiant ou mot de passe incorrect", HttpStatus.UNAUTHORIZED));
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new ApiException("AUTH_INVALID_CREDENTIALS",
                    "Identifiant ou mot de passe incorrect", HttpStatus.UNAUTHORIZED);
        }
        if (!user.isActive()) {
            throw new ApiException("AUTH_ACCOUNT_DISABLED",
                    "Compte desactive", HttpStatus.FORBIDDEN);
        }
        if (!user.isPhoneVerified() && !user.isEmailVerified()) {
            throw new ApiException("AUTH_PHONE_NOT_VERIFIED",
                    "Compte non verifie. Validez votre OTP.", HttpStatus.FORBIDDEN);
        }
        auditService.record(user.getId(), "AUTH_LOGIN", "User", user.getId().toString(), null, null);
        return buildSession(user);
    }

    @Transactional
    public IdentifierResponse forgotPassword(ForgotPasswordRequest req) {
        // Reponse identique meme si l'utilisateur est inconnu (anti enumeration).
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
        // JWT stateless : la revocation cote serveur necessite une liste noire.
        // A implementer si besoin (table revoked_tokens). Ici, action tracee uniquement.
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

    /**
     * Union des rôles globaux du User et des rôles de chaque Membership ACTIVE.
     * Permet au frontend d'afficher les menus liés aux rôles tontine sans appel
     * supplémentaire. L'autorisation effective reste vérifiée côté serveur par
     * les *AccessChecker (Président, Censeur, ...) qui contrôlent le rôle dans la
     * tontine ciblée — un user PRESIDENT de tontine A et MEMBRE de tontine B aura
     * "PRESIDENT" dans User.roles mais sera refusé sur /president/* de la tontine B.
     */
    private Set<UserRole> aggregateRoles(UserEntity user) {
        EnumSet<UserRole> roles = EnumSet.copyOf(user.getRoles());
        for (Member m : memberRepository.findByUserId(user.getId())) {
            if (m.getStatus() == MemberStatus.ACTIVE) {
                roles.addAll(m.getRoles());
            }
        }
        return roles;
    }

    private boolean isEmail(String identifier) {
        return identifier != null && identifier.contains("@");
    }
}
