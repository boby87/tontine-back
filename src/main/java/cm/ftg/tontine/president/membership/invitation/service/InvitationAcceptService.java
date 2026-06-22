package cm.ftg.tontine.president.membership.invitation.service;

import cm.ftg.tontine.audit.service.AuditService;
import cm.ftg.tontine.auth.dto.AuthSessionDto;
import cm.ftg.tontine.auth.dto.TokensDto;
import cm.ftg.tontine.auth.dto.UserDto;
import cm.ftg.tontine.auth.entity.UserEntity;
import cm.ftg.tontine.auth.repository.UserRepository;
import cm.ftg.tontine.common.enums.MemberStatus;
import cm.ftg.tontine.common.enums.UserRole;
import cm.ftg.tontine.common.exception.ApiException;
import cm.ftg.tontine.common.exception.ResourceNotFoundException;
import cm.ftg.tontine.member.entity.Member;
import cm.ftg.tontine.member.repository.MemberRepository;
import cm.ftg.tontine.notification.enums.NotificationCategory;
import cm.ftg.tontine.notification.enums.NotificationKind;
import cm.ftg.tontine.notification.service.NotificationService;
import cm.ftg.tontine.president.membership.invitation.dto.AcceptInvitationRequest;
import cm.ftg.tontine.president.membership.invitation.dto.AcceptInvitationResponse;
import cm.ftg.tontine.president.membership.invitation.dto.InvitationPreviewDto;
import cm.ftg.tontine.president.membership.invitation.entity.MembershipInvitation;
import cm.ftg.tontine.president.membership.invitation.enums.InvitationStatus;
import cm.ftg.tontine.president.membership.invitation.repository.MembershipInvitationRepository;
import cm.ftg.tontine.realtime.RealtimeEventPublisher;
import cm.ftg.tontine.security.JwtService;
import cm.ftg.tontine.tontine.entity.Tontine;
import cm.ftg.tontine.tontine.repository.TontineRepository;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Workflow public d'acceptation d'une invitation : preview et accept.
 * L'acceptation cree (ou reutilise) le compte User et le Member ACTIVE, puis
 * emet une session JWT identique a {@code /auth/login}.
 */
@Service
public class InvitationAcceptService {

    private static final HttpStatus UNPROCESSABLE = HttpStatus.valueOf(422);

    private final MembershipInvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final MemberRepository memberRepository;
    private final TontineRepository tontineRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditService auditService;
    private final RealtimeEventPublisher realtime;
    private final NotificationService notificationService;

    public InvitationAcceptService(MembershipInvitationRepository invitationRepository,
                                   UserRepository userRepository,
                                   MemberRepository memberRepository,
                                   TontineRepository tontineRepository,
                                   PasswordEncoder passwordEncoder,
                                   JwtService jwtService,
                                   AuditService auditService,
                                   RealtimeEventPublisher realtime,
                                   NotificationService notificationService) {
        this.invitationRepository = invitationRepository;
        this.userRepository = userRepository;
        this.memberRepository = memberRepository;
        this.tontineRepository = tontineRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.auditService = auditService;
        this.realtime = realtime;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public InvitationPreviewDto preview(String token) {
        MembershipInvitation inv = invitationRepository.findByToken(token)
                .orElseThrow(() -> new ApiException("INVITATION_NOT_FOUND",
                        "Invitation introuvable", HttpStatus.NOT_FOUND));
        Tontine tontine = tontineRepository.findById(inv.getTontineId()).orElse(null);
        boolean expired = inv.getStatus() == InvitationStatus.EXPIRED
                || inv.getExpiresAt().isBefore(Instant.now());
        boolean alreadyAccepted = inv.getStatus() == InvitationStatus.ACCEPTED;
        return new InvitationPreviewDto(
                tontine == null ? null : tontine.getName(),
                inv.getInvitedByFullName(),
                inv.getProposedRole(),
                inv.getCandidateFullName(),
                inv.getCandidatePhone(),
                inv.getCandidateEmail(),
                inv.getExpiresAt(),
                expired,
                alreadyAccepted);
    }

    @Transactional
    public AcceptInvitationResponse accept(String token, AcceptInvitationRequest req) {
        MembershipInvitation inv = invitationRepository.findByToken(token)
                .orElseThrow(() -> new ApiException("INVITATION_NOT_FOUND",
                        "Invitation introuvable", HttpStatus.NOT_FOUND));
        if (inv.getStatus() == InvitationStatus.CANCELLED) {
            throw new ApiException("INVITATION_CANCELLED",
                    "Cette invitation a ete annulee", UNPROCESSABLE);
        }
        if (inv.getStatus() == InvitationStatus.ACCEPTED) {
            throw new ApiException("INVITATION_ALREADY_ACCEPTED",
                    "Cette invitation a deja ete acceptee", HttpStatus.CONFLICT);
        }
        if (inv.getStatus() == InvitationStatus.EXPIRED || inv.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiException("INVITATION_EXPIRED",
                    "Cette invitation a expire", UNPROCESSABLE);
        }
        Tontine tontine = tontineRepository.findById(inv.getTontineId())
                .orElseThrow(() -> new ResourceNotFoundException("Tontine", inv.getTontineId()));

        UserEntity user = resolveOrCreateUser(inv, req.password());
        Member member = resolveOrCreateMember(inv, user);

        if (user.getActiveTontineId() == null) {
            user.setActiveTontineId(tontine.getId());
            user = userRepository.save(user);
        }

        Instant now = Instant.now();
        inv.setStatus(InvitationStatus.ACCEPTED);
        inv.setAcceptedAt(now);
        inv.setAcceptedUserId(user.getId());
        invitationRepository.save(inv);

        tontine.setMemberCount((int) memberRepository.countByTontineId(tontine.getId()));
        tontineRepository.save(tontine);

        auditService.record(user.getId(), "INVITATION_ACCEPT", "MembershipInvitation",
                inv.getId().toString(), tontine.getId(), null);
        realtime.toTontine(tontine.getId(), "invitation.accepted", inv.getId());
        realtime.toTontine(tontine.getId(), "tontine.member-added", member.getId());

        // Notifie le président en in-app : son panneau de notifications se met à jour
        userRepository.findById(inv.getInvitedByUserId()).ifPresent(president ->
                notificationService.publish(
                        president.getId(),
                        tontine.getId(),
                        NotificationKind.INFO,
                        NotificationCategory.INVITATION,
                        "Invitation acceptée",
                        inv.getCandidateFullName() + " a accepté votre invitation et a rejoint la tontine \""
                                + tontine.getName() + "\".",
                        null));

        return new AcceptInvitationResponse(buildSession(user), member.getId(),
                tontine.getId(), tontine.getName());
    }

    private UserEntity resolveOrCreateUser(MembershipInvitation inv, String rawPassword) {
        UserEntity existing = userRepository.findByPhone(inv.getCandidatePhone()).orElse(null);
        if (existing == null && inv.getCandidateEmail() != null && !inv.getCandidateEmail().isBlank()) {
            existing = userRepository.findByEmail(inv.getCandidateEmail().toLowerCase().trim()).orElse(null);
        }
        if (existing != null) {
            return existing;
        }
        String[] parts = inv.getCandidateFullName().trim().split(" ", 2);
        UserEntity u = new UserEntity();
        u.setFirstName(parts[0]);
        u.setLastName(parts.length > 1 ? parts[1] : "");
        u.setEmail(resolveEmail(inv));
        u.setPhone(inv.getCandidatePhone());
        u.setPasswordHash(passwordEncoder.encode(rawPassword));
        u.setRoles(EnumSet.of(UserRole.MEMBER));
        u.setActive(true);
        // Cliquer sur le lien recu par SMS vaut verification du telephone.
        u.setPhoneVerified(true);
        u.setEmailVerified(inv.getCandidateEmail() != null && !inv.getCandidateEmail().isBlank());
        return userRepository.save(u);
    }

    private Member resolveOrCreateMember(MembershipInvitation inv, UserEntity user) {
        Member existing = memberRepository.findByUserIdAndTontineId(user.getId(), inv.getTontineId()).orElse(null);
        if (existing != null) {
            return existing;
        }
        Member m = new Member();
        m.setUserId(user.getId());
        m.setTontineId(inv.getTontineId());
        m.setMatricule("M-%03d".formatted((int) memberRepository.countByTontineId(inv.getTontineId()) + 1));
        m.setFirstName(user.getFirstName());
        m.setLastName(user.getLastName());
        m.setPhone(inv.getCandidatePhone());
        m.setEmail(inv.getCandidateEmail());
        m.setStatus(MemberStatus.ACTIVE);
        m.setRoles(EnumSet.of(inv.getProposedRole(), UserRole.MEMBER));
        return memberRepository.save(m);
    }

    /** UserEntity.email est NOT NULL/unique : synthetiser une adresse si l'invitation n'en a pas. */
    private String resolveEmail(MembershipInvitation inv) {
        if (inv.getCandidateEmail() != null && !inv.getCandidateEmail().isBlank()) {
            return inv.getCandidateEmail().toLowerCase().trim();
        }
        return "invite-" + inv.getCandidatePhone().replaceAll("\\D", "") + "@invitation.local";
    }

    private AuthSessionDto buildSession(UserEntity user) {
        TokensDto tokens = new TokensDto(
                jwtService.generateAccessToken(user),
                jwtService.generateRefreshToken(user),
                jwtService.accessTokenTtlSeconds());
        return new AuthSessionDto(UserDto.from(user, aggregateRoles(user)), tokens, user.getActiveTontineId());
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
}
