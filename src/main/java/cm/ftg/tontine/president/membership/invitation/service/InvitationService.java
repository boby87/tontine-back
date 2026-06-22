package cm.ftg.tontine.president.membership.invitation.service;

import cm.ftg.tontine.audit.service.AuditService;
import cm.ftg.tontine.auth.entity.UserEntity;
import cm.ftg.tontine.auth.repository.UserRepository;
import cm.ftg.tontine.common.enums.MemberStatus;
import cm.ftg.tontine.common.enums.TontineStatus;
import cm.ftg.tontine.common.enums.UserRole;
import cm.ftg.tontine.common.exception.ApiException;
import cm.ftg.tontine.common.exception.ResourceNotFoundException;
import cm.ftg.tontine.member.repository.MemberRepository;
import cm.ftg.tontine.notification.enums.NotificationCategory;
import cm.ftg.tontine.notification.enums.NotificationKind;
import cm.ftg.tontine.notification.service.NotificationService;
import cm.ftg.tontine.president.membership.invitation.dto.CancelInvitationRequest;
import cm.ftg.tontine.president.membership.invitation.dto.CandidateLookupDto;
import cm.ftg.tontine.president.membership.invitation.dto.InviteMemberRequest;
import cm.ftg.tontine.president.membership.invitation.dto.MembershipInvitationDto;
import cm.ftg.tontine.president.membership.invitation.entity.MembershipInvitation;
import cm.ftg.tontine.president.membership.invitation.enums.InvitationChannel;
import cm.ftg.tontine.president.membership.invitation.enums.InvitationStatus;
import cm.ftg.tontine.president.membership.invitation.repository.MembershipInvitationRepository;
import cm.ftg.tontine.president.security.PresidentAccessChecker;
import cm.ftg.tontine.realtime.RealtimeEventPublisher;
import cm.ftg.tontine.tontine.dto.FounderInviteDto;
import cm.ftg.tontine.tontine.entity.Tontine;
import cm.ftg.tontine.tontine.repository.TontineRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cycle de vie cote President : creation, listing, relance et annulation des invitations.
 * Aligne sur les patterns de {@code president.vote.*} (PresidentAccessChecker + AuditService
 * + RealtimeEventPublisher).
 */
@Service
public class InvitationService {

    private static final HttpStatus UNPROCESSABLE = HttpStatus.valueOf(422);
    private static final Set<TontineStatus> INVITABLE_TONTINE = Set.of(
            TontineStatus.DRAFT, TontineStatus.ACTIVE);
    private static final Set<InvitationStatus> OPEN_STATUSES = Set.of(
            InvitationStatus.PENDING, InvitationStatus.SENT);

    private final MembershipInvitationRepository invitationRepository;
    private final MemberRepository memberRepository;
    private final TontineRepository tontineRepository;
    private final UserRepository userRepository;
    private final PresidentAccessChecker accessChecker;
    private final AuditService auditService;
    private final RealtimeEventPublisher realtime;
    private final InvitationTokenGenerator tokenGenerator;
    private final InvitationDispatcher dispatcher;
    private final NotificationService notificationService;
    private final int expiryDays;
    private final String baseAcceptUrl;

    public InvitationService(MembershipInvitationRepository invitationRepository,
                             MemberRepository memberRepository,
                             TontineRepository tontineRepository,
                             UserRepository userRepository,
                             PresidentAccessChecker accessChecker,
                             AuditService auditService,
                             RealtimeEventPublisher realtime,
                             InvitationTokenGenerator tokenGenerator,
                             InvitationDispatcher dispatcher,
                             NotificationService notificationService,
                             @Value("${app.invitation.expiry-days:7}") int expiryDays,
                             @Value("${app.frontend.base-url:https://app.tontine-connect.cm}") String frontendBaseUrl) {
        this.invitationRepository = invitationRepository;
        this.memberRepository = memberRepository;
        this.tontineRepository = tontineRepository;
        this.userRepository = userRepository;
        this.accessChecker = accessChecker;
        this.auditService = auditService;
        this.realtime = realtime;
        this.tokenGenerator = tokenGenerator;
        this.dispatcher = dispatcher;
        this.notificationService = notificationService;
        this.expiryDays = expiryDays;
        this.baseAcceptUrl = frontendBaseUrl + "/auth";
    }

    /**
     * Recherche un utilisateur existant a inviter, soit par son identifiant (UUID), soit par
     * son numero de telephone. Reserve au President de la tontine. Sert au pre-remplissage du
     * formulaire d'invitation cote front.
     */
    @Transactional(readOnly = true)
    public CandidateLookupDto lookupCandidate(UUID tontineId, UUID requesterId, String userId, String phone) {
        accessChecker.requirePresident(requesterId, tontineId);
        UserEntity u;
        if (userId != null && !userId.isBlank()) {
            UUID id;
            try {
                id = UUID.fromString(userId.trim());
            } catch (IllegalArgumentException e) {
                throw new ApiException("INVALID_IDENTIFIER",
                        "Identifiant utilisateur invalide", UNPROCESSABLE);
            }
            u = userRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", id));
        } else if (phone != null && !phone.isBlank()) {
            String normalized = phone.trim();
            u = userRepository.findByPhone(normalized)
                    .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", normalized));
        } else {
            throw new ApiException("MISSING_IDENTIFIER",
                    "Fournir un identifiant ou un numero de telephone", UNPROCESSABLE);
        }
        MemberStatus memberStatus = memberRepository.findByUserIdAndTontineId(u.getId(), tontineId)
                .map(m -> m.getStatus())
                .orElse(null);
        boolean alreadyMember = memberStatus == MemberStatus.ACTIVE
                || memberStatus == MemberStatus.PENDING
                || memberStatus == MemberStatus.SUSPENDED;
        return new CandidateLookupDto(
                u.getId(), u.getFirstName(), u.getLastName(),
                u.getPhone(), u.getEmail(), alreadyMember, memberStatus);
    }

    @Transactional
    public MembershipInvitationDto invite(UUID tontineId, UUID userId, InviteMemberRequest req) {
        accessChecker.requirePresident(userId, tontineId);
        Tontine tontine = tontineRepository.findById(tontineId)
                .orElseThrow(() -> new ResourceNotFoundException("Tontine", tontineId));

        if (!INVITABLE_TONTINE.contains(tontine.getStatus())) {
            throw new ApiException("TONTINE_INACTIVE", "La tontine n'est pas active", UNPROCESSABLE);
        }
        if (req.proposedRole() == UserRole.PRESIDENT) {
            throw new ApiException("INVITATION_PRESIDENT_FORBIDDEN",
                    "Le createur de la tontine est l'unique President. Pour transferer le role, "
                            + "utilisez le module Delegations.", UNPROCESSABLE);
        }
        long members = memberRepository.countByTontineId(tontineId);
        long pendingSent = invitationRepository.countByTontineIdAndStatus(tontineId, InvitationStatus.SENT);
        if (members + pendingSent >= tontine.getMaxMembers()) {
            throw new ApiException("TONTINE_FULL",
                    "Le nombre maximum de membres est atteint", UNPROCESSABLE);
        }
        if (memberRepository.existsByTontineIdAndPhoneAndStatus(tontineId, req.candidatePhone(), MemberStatus.ACTIVE)) {
            throw new ApiException("MEMBER_ALREADY_EXISTS",
                    "Un membre avec ce numero existe deja", HttpStatus.CONFLICT);
        }
        if (invitationRepository.existsByTontineIdAndCandidatePhoneAndStatusIn(
                tontineId, req.candidatePhone(), OPEN_STATUSES)) {
            throw new ApiException("INVITATION_ALREADY_PENDING",
                    "Une invitation est deja en cours pour ce numero", HttpStatus.CONFLICT);
        }
        UserEntity inviter = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        MembershipInvitation inv = createAndDispatch(tontine, userId, fullName(inviter),
                req.candidateFullName(), req.candidatePhone(), req.candidateEmail(),
                req.proposedRole(), req.channels(), req.message());
        return toDto(inv);
    }

    @Transactional(readOnly = true)
    public List<MembershipInvitationDto> list(UUID tontineId, UUID userId) {
        accessChecker.requirePresident(userId, tontineId);
        return invitationRepository.findByTontineIdOrderByInvitedAtDesc(tontineId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public MembershipInvitationDto resend(UUID id, UUID tontineId, UUID userId) {
        accessChecker.requirePresident(userId, tontineId);
        MembershipInvitation inv = loadInTontine(id, tontineId);
        if (inv.getStatus() != InvitationStatus.SENT && inv.getStatus() != InvitationStatus.PENDING) {
            throw new ApiException("INVITATION_NOT_RESENDABLE",
                    "Seules les invitations en attente peuvent etre relancees", UNPROCESSABLE);
        }
        Tontine tontine = tontineRepository.findById(tontineId)
                .orElseThrow(() -> new ResourceNotFoundException("Tontine", tontineId));
        Instant now = Instant.now();
        if (inv.getExpiresAt().isBefore(now)) {
            inv.setExpiresAt(now.plus(expiryDays, ChronoUnit.DAYS));
        }
        boolean sent = dispatcher.dispatch(inv, tontine.getName());
        inv.setRemindersSent(inv.getRemindersSent() + 1);
        if (sent) {
            inv.setStatus(InvitationStatus.SENT);
            inv.setSentAt(now);
        }
        MembershipInvitation saved = invitationRepository.save(inv);
        auditService.record(userId, "INVITATION_RESEND", "MembershipInvitation",
                saved.getId().toString(), tontineId, null);
        realtime.toTontine(tontineId, "invitation.resent", toDto(saved));
        return toDto(saved);
    }

    @Transactional
    public MembershipInvitationDto cancel(UUID id, UUID tontineId, UUID userId, CancelInvitationRequest req) {
        accessChecker.requirePresident(userId, tontineId);
        MembershipInvitation inv = loadInTontine(id, tontineId);
        if (inv.getStatus() == InvitationStatus.ACCEPTED) {
            throw new ApiException("INVITATION_ALREADY_ACCEPTED",
                    "Cette invitation a deja ete acceptee", UNPROCESSABLE);
        }
        inv.setStatus(InvitationStatus.CANCELLED);
        inv.setCancelledAt(Instant.now());
        inv.setCancelledByUserId(userId);
        inv.setCancelReason(req == null ? null : req.reason());
        MembershipInvitation saved = invitationRepository.save(inv);
        auditService.record(userId, "INVITATION_CANCEL", "MembershipInvitation",
                saved.getId().toString(), tontineId, null);
        realtime.toTontine(tontineId, "invitation.cancelled", toDto(saved));
        return toDto(saved);
    }

    /**
     * Hook appele par {@code TontineService.create()} pour auto-inviter un fondateur
     * (different du createur). Pas de controle de role : le createur est President par
     * construction.
     */
    @Transactional
    public void inviteFromFounder(UUID tontineId, UUID creatorUserId, UserEntity creator,
                                  FounderInviteDto founder, Set<InvitationChannel> channels) {
        Tontine tontine = tontineRepository.findById(tontineId)
                .orElseThrow(() -> new ResourceNotFoundException("Tontine", tontineId));
        createAndDispatch(tontine, creatorUserId, fullName(creator),
                founder.fullName(), founder.phone(), founder.email(),
                founder.role(), channels, null);
    }

    private MembershipInvitation createAndDispatch(Tontine tontine, UUID invitedByUserId,
                                                   String invitedByFullName, String candidateFullName,
                                                   String candidatePhone, String candidateEmail,
                                                   UserRole proposedRole, Set<InvitationChannel> channels,
                                                   String message) {
        MembershipInvitation inv = new MembershipInvitation();
        inv.setTontineId(tontine.getId());
        inv.setCandidateFullName(candidateFullName);
        inv.setCandidatePhone(candidatePhone);
        inv.setCandidateEmail(candidateEmail);
        inv.setProposedRole(proposedRole);
        inv.setChannels(channels.stream().map(Enum::name).collect(Collectors.joining(",")));
        inv.setMessage(message);
        inv.setStatus(InvitationStatus.PENDING);
        inv.setToken(tokenGenerator.generate());
        inv.setExpiresAt(Instant.now().plus(expiryDays, ChronoUnit.DAYS));
        inv.setInvitedByUserId(invitedByUserId);
        inv.setInvitedByFullName(invitedByFullName);
        MembershipInvitation saved = invitationRepository.save(inv);

        boolean sent = dispatcher.dispatch(saved, tontine.getName());
        if (sent) {
            saved.setStatus(InvitationStatus.SENT);
            saved.setSentAt(Instant.now());
            saved = invitationRepository.save(saved);
        }
        auditService.record(invitedByUserId, "INVITATION_CREATE", "MembershipInvitation",
                saved.getId().toString(), tontine.getId(),
                "{\"phone\":\"" + candidatePhone + "\"}");
        realtime.toTontine(tontine.getId(), "invitation.created", toDto(saved));

        // Notifie le candidat en in-app s'il a déjà un compte.
        // Normalisation du téléphone : supprime espaces/tirets pour éviter les faux-négatifs
        // (ex: "+237 600 000 000" vs "+237600000000").
        final MembershipInvitation finalSaved = saved;
        String normalizedPhone = candidatePhone == null ? null
                : candidatePhone.replaceAll("[\\s\\-]", "");
        userRepository.findByPhone(normalizedPhone).ifPresent(existing ->
                notificationService.publish(
                        existing.getId(),
                        tontine.getId(),
                        NotificationKind.INFO,
                        NotificationCategory.INVITATION,
                        "Invitation à rejoindre une tontine",
                        invitedByFullName + " vous invite à rejoindre la tontine \""
                                + tontine.getName() + "\" en tant que "
                                + roleLabel(proposedRole) + ".",
                        baseAcceptUrl + "/invitations/" + finalSaved.getToken() + "/accept"));

        return saved;
    }

    private MembershipInvitation loadInTontine(UUID id, UUID tontineId) {
        MembershipInvitation inv = invitationRepository.findById(id)
                .orElseThrow(() -> new ApiException("INVITATION_NOT_FOUND",
                        "Invitation introuvable", HttpStatus.NOT_FOUND));
        if (!inv.getTontineId().equals(tontineId)) {
            throw new ApiException("FORBIDDEN", "Tontine non concordante", HttpStatus.FORBIDDEN);
        }
        return inv;
    }

    private MembershipInvitationDto toDto(MembershipInvitation inv) {
        return MembershipInvitationDto.from(inv, baseAcceptUrl);
    }

    private String roleLabel(UserRole role) {
        return switch (role) {
            case PRESIDENT -> "Président";
            case SECRETARY -> "Secrétaire";
            case TREASURER -> "Trésorier";
            case CENSOR -> "Censeur";
            case AUDITOR -> "Commissaire aux comptes";
            case ADMIN -> "Administrateur";
            case MEMBER -> "Membre";
        };
    }

    private String fullName(UserEntity u) {
        String first = u.getFirstName() == null ? "" : u.getFirstName();
        String last = u.getLastName() == null ? "" : u.getLastName();
        String full = (first + " " + last).trim();
        return full.isEmpty() ? u.getEmail() : full;
    }
}
