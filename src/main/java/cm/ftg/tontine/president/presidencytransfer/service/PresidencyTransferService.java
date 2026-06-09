package cm.ftg.tontine.president.presidencytransfer.service;

import cm.ftg.tontine.audit.service.AuditService;
import cm.ftg.tontine.auth.entity.UserEntity;
import cm.ftg.tontine.auth.repository.UserRepository;
import cm.ftg.tontine.common.enums.MemberStatus;
import cm.ftg.tontine.common.enums.UserRole;
import cm.ftg.tontine.common.exception.ApiException;
import cm.ftg.tontine.integration.messaging.email.EmailSender;
import cm.ftg.tontine.integration.messaging.sms.SmsSender;
import cm.ftg.tontine.member.entity.Member;
import cm.ftg.tontine.member.repository.MemberRepository;
import cm.ftg.tontine.notification.enums.NotificationCategory;
import cm.ftg.tontine.notification.enums.NotificationKind;
import cm.ftg.tontine.notification.service.NotificationService;
import cm.ftg.tontine.president.presidencytransfer.dto.CancelPresidencyTransferRequest;
import cm.ftg.tontine.president.presidencytransfer.dto.DeclinePresidencyTransferRequest;
import cm.ftg.tontine.president.presidencytransfer.dto.InitiatePresidencyTransferRequest;
import cm.ftg.tontine.president.presidencytransfer.dto.PresidencyTransferDto;
import cm.ftg.tontine.president.presidencytransfer.entity.PresidencyTransfer;
import cm.ftg.tontine.president.presidencytransfer.enums.PresidencyTransferStatus;
import cm.ftg.tontine.president.presidencytransfer.repository.PresidencyTransferRepository;
import cm.ftg.tontine.president.security.PresidentAccessChecker;
import cm.ftg.tontine.realtime.RealtimeEventPublisher;
import cm.ftg.tontine.tontine.entity.Tontine;
import cm.ftg.tontine.tontine.repository.TontineRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Workflow du transfert de presidence cote initiateur (President) et consultation/refus
 * cote membre cible. Le swap atomique des roles est isole dans {@link PresidencyAcceptService}.
 * Aligne sur {@code president.delegation.*}.
 */
@Service
public class PresidencyTransferService {

    private static final Logger log = LoggerFactory.getLogger(PresidencyTransferService.class);
    private static final HttpStatus UNPROCESSABLE = HttpStatus.valueOf(422);
    private static final String MEMBER_QUEUE = "/queue/presidency-transfer";

    private final PresidencyTransferRepository transferRepository;
    private final MemberRepository memberRepository;
    private final UserRepository userRepository;
    private final TontineRepository tontineRepository;
    private final PresidentAccessChecker accessChecker;
    private final AuditService auditService;
    private final RealtimeEventPublisher realtime;
    private final NotificationService notificationService;
    private final SmsSender smsSender;
    private final EmailSender emailSender;
    private final long expiryHours;
    private final String frontendBaseUrl;

    public PresidencyTransferService(PresidencyTransferRepository transferRepository,
                                     MemberRepository memberRepository,
                                     UserRepository userRepository,
                                     TontineRepository tontineRepository,
                                     PresidentAccessChecker accessChecker,
                                     AuditService auditService,
                                     RealtimeEventPublisher realtime,
                                     NotificationService notificationService,
                                     SmsSender smsSender,
                                     EmailSender emailSender,
                                     @Value("${app.presidency-transfer.expiry-hours:72}") long expiryHours,
                                     @Value("${app.frontend.base-url:https://app.tontine-connect.cm}") String frontendBaseUrl) {
        this.transferRepository = transferRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.tontineRepository = tontineRepository;
        this.accessChecker = accessChecker;
        this.auditService = auditService;
        this.realtime = realtime;
        this.notificationService = notificationService;
        this.smsSender = smsSender;
        this.emailSender = emailSender;
        this.expiryHours = expiryHours;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    @Transactional
    public PresidencyTransferDto initiate(UUID tontineId, UUID userId, InitiatePresidencyTransferRequest req) {
        accessChecker.requirePresident(userId, tontineId);

        if (transferRepository.findFirstByTontineIdAndStatus(tontineId, PresidencyTransferStatus.PENDING).isPresent()) {
            throw new ApiException("PRESIDENCY_TRANSFER_ALREADY_PENDING",
                    "Un transfert est deja en attente. Annulez-le avant d'en creer un nouveau",
                    HttpStatus.CONFLICT);
        }
        Member target = memberRepository.findByIdAndTontineId(req.targetMemberId(), tontineId)
                .orElseThrow(() -> new ApiException("TARGET_MEMBER_NOT_ELIGIBLE",
                        "Le membre designe doit etre actif", UNPROCESSABLE));
        if (target.getStatus() != MemberStatus.ACTIVE) {
            throw new ApiException("TARGET_MEMBER_NOT_ELIGIBLE",
                    "Le membre designe doit etre actif", UNPROCESSABLE);
        }
        if (target.getUserId() != null && target.getUserId().equals(userId)) {
            throw new ApiException("TARGET_CANNOT_BE_SELF",
                    "Vous ne pouvez pas vous designer vous-meme", UNPROCESSABLE);
        }
        if (target.getRoles().contains(UserRole.PRESIDENT)) {
            throw new ApiException("TARGET_ALREADY_PRESIDENT",
                    "Le membre designe est deja President", UNPROCESSABLE);
        }
        UserEntity initiator = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("FORBIDDEN",
                        "Initiateur introuvable", HttpStatus.FORBIDDEN));
        String tontineName = tontineRepository.findById(tontineId)
                .map(Tontine::getName).orElse("");

        PresidencyTransfer t = new PresidencyTransfer();
        t.setTontineId(tontineId);
        t.setInitiatedByUserId(userId);
        t.setInitiatedByFullName(buildUserName(initiator));
        t.setTargetMemberId(target.getId());
        t.setTargetUserId(target.getUserId());
        t.setTargetMemberFullName(buildMemberName(target));
        t.setReason(req.reason());
        t.setStatus(PresidencyTransferStatus.PENDING);
        t.setExpiresAt(Instant.now().plus(expiryHours, ChronoUnit.HOURS));
        PresidencyTransfer saved = transferRepository.save(t);

        notifyUser(saved.getTargetUserId(), tontineId, NotificationKind.WARNING,
                "Proposition de succession a la presidence",
                "Le President " + saved.getInitiatedByFullName() + " de la tontine " + tontineName
                        + " vous propose de prendre sa succession.",
                frontendBaseUrl + "/member/presidency-transfer",
                "Le President " + saved.getInitiatedByFullName() + " de la tontine " + tontineName
                        + " vous propose de prendre sa succession. Vous avez jusqu'au " + saved.getExpiresAt()
                        + " pour decider. Lien : " + frontendBaseUrl + "/member/presidency-transfer",
                "Proposition de succession a la presidence");

        auditService.record(userId, "PRESIDENCY_TRANSFER_INIT", "PresidencyTransfer",
                saved.getId().toString(), tontineId, "{\"target\":\"" + saved.getTargetMemberId() + "\"}");
        realtime.toUser(saved.getTargetUserId(), MEMBER_QUEUE, "presidency-transfer.proposed",
                PresidencyTransferDto.from(saved));
        return PresidencyTransferDto.from(saved);
    }

    @Transactional(readOnly = true)
    public List<PresidencyTransferDto> list(UUID tontineId, UUID userId) {
        accessChecker.requirePresident(userId, tontineId);
        return transferRepository.findByTontineIdOrderByInitiatedAtDesc(tontineId).stream()
                .map(PresidencyTransferDto::from)
                .toList();
    }

    @Transactional
    public PresidencyTransferDto cancel(UUID id, UUID tontineId, UUID userId, CancelPresidencyTransferRequest req) {
        accessChecker.requirePresident(userId, tontineId);
        PresidencyTransfer t = loadInTontine(id, tontineId);
        if (t.getStatus() != PresidencyTransferStatus.PENDING) {
            throw new ApiException("TRANSFER_NOT_PENDING",
                    "Ce transfert n'est plus en attente", UNPROCESSABLE);
        }
        t.setStatus(PresidencyTransferStatus.CANCELLED);
        t.setCancelledAt(Instant.now());
        t.setCancelledByUserId(userId);
        t.setCancelReason(req == null ? null : req.reason());
        PresidencyTransfer saved = transferRepository.save(t);

        notifyUser(saved.getTargetUserId(), tontineId, NotificationKind.INFO,
                "Transfert de presidence annule",
                "Le President a annule la proposition de succession.",
                frontendBaseUrl + "/member/presidency-transfer",
                "Le President a annule la proposition de succession.",
                "Transfert de presidence annule");

        auditService.record(userId, "PRESIDENCY_TRANSFER_CANCEL", "PresidencyTransfer",
                saved.getId().toString(), tontineId, null);
        realtime.toUser(saved.getTargetUserId(), MEMBER_QUEUE, "presidency-transfer.cancelled",
                PresidencyTransferDto.from(saved));
        return PresidencyTransferDto.from(saved);
    }

    @Transactional
    public PresidencyTransferDto getPendingForMember(UUID userId) {
        PresidencyTransfer t = transferRepository
                .findFirstByTargetUserIdAndStatus(userId, PresidencyTransferStatus.PENDING)
                .orElse(null);
        if (t == null) {
            return null;
        }
        if (t.getExpiresAt().isBefore(Instant.now())) {
            t.setStatus(PresidencyTransferStatus.EXPIRED);
            transferRepository.save(t);
            return null;
        }
        return PresidencyTransferDto.from(t);
    }

    @Transactional
    public PresidencyTransferDto decline(UUID id, UUID userId, DeclinePresidencyTransferRequest req) {
        PresidencyTransfer t = transferRepository.findById(id)
                .orElseThrow(() -> new ApiException("PRESIDENCY_TRANSFER_NOT_FOUND",
                        "Transfert introuvable", HttpStatus.NOT_FOUND));
        if (t.getTargetUserId() == null || !t.getTargetUserId().equals(userId)) {
            throw new ApiException("FORBIDDEN",
                    "Vous n'etes pas le destinataire de cette proposition", HttpStatus.FORBIDDEN);
        }
        if (t.getStatus() != PresidencyTransferStatus.PENDING) {
            throw new ApiException("TRANSFER_NOT_PENDING",
                    "Ce transfert n'est plus en attente", UNPROCESSABLE);
        }
        t.setStatus(PresidencyTransferStatus.DECLINED);
        t.setDeclinedAt(Instant.now());
        t.setDeclineReason(req.reason());
        PresidencyTransfer saved = transferRepository.save(t);

        notifyUser(saved.getInitiatedByUserId(), saved.getTontineId(), NotificationKind.WARNING,
                "Transfert de presidence refuse",
                saved.getTargetMemberFullName() + " a refuse la proposition de succession.",
                frontendBaseUrl + "/president/presidency-transfer",
                saved.getTargetMemberFullName() + " a refuse la proposition de succession. Motif : "
                        + saved.getDeclineReason(),
                "Transfert de presidence refuse");

        auditService.record(userId, "PRESIDENCY_TRANSFER_DECLINE", "PresidencyTransfer",
                saved.getId().toString(), saved.getTontineId(), null);
        realtime.toUser(saved.getInitiatedByUserId(), MEMBER_QUEUE, "presidency-transfer.declined",
                PresidencyTransferDto.from(saved));
        return PresidencyTransferDto.from(saved);
    }

    private PresidencyTransfer loadInTontine(UUID id, UUID tontineId) {
        PresidencyTransfer t = transferRepository.findById(id)
                .orElseThrow(() -> new ApiException("PRESIDENCY_TRANSFER_NOT_FOUND",
                        "Transfert introuvable", HttpStatus.NOT_FOUND));
        if (!t.getTontineId().equals(tontineId)) {
            throw new ApiException("FORBIDDEN", "Tontine non concordante", HttpStatus.FORBIDDEN);
        }
        return t;
    }

    /** In-app (toujours) + SMS + e-mail (best-effort) vers le user cible s'il existe. */
    private void notifyUser(UUID userId, UUID tontineId, NotificationKind kind, String title,
                            String inAppMessage, String link, String smsBody, String emailSubject) {
        if (userId == null) {
            return;
        }
        notificationService.publish(userId, tontineId, kind, NotificationCategory.GENERAL,
                title, inAppMessage, link);
        userRepository.findById(userId).ifPresent(u -> {
            if (u.getPhone() != null && !u.getPhone().isBlank()) {
                try {
                    smsSender.send(u.getPhone(), smsBody);
                } catch (RuntimeException ex) {
                    log.warn("[PresidencyTransfer] echec SMS user={} : {}", userId, ex.getMessage());
                }
            }
            if (u.getEmail() != null && !u.getEmail().isBlank()) {
                try {
                    emailSender.send(u.getEmail(), emailSubject, "<p>" + inAppMessage + "</p>");
                } catch (RuntimeException ex) {
                    log.warn("[PresidencyTransfer] echec e-mail user={} : {}", userId, ex.getMessage());
                }
            }
        });
    }

    private String buildUserName(UserEntity u) {
        String first = u.getFirstName() == null ? "" : u.getFirstName();
        String last = u.getLastName() == null ? "" : u.getLastName();
        String full = (first + " " + last).trim();
        return full.isEmpty() ? u.getEmail() : full;
    }

    private String buildMemberName(Member m) {
        String first = m.getFirstName() == null ? "" : m.getFirstName();
        String last = m.getLastName() == null ? "" : m.getLastName();
        String full = (first + " " + last).trim();
        return full.isEmpty() ? m.getMatricule() : full;
    }
}
