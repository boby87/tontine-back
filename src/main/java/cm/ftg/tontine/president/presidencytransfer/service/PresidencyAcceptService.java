package cm.ftg.tontine.president.presidencytransfer.service;

import cm.ftg.tontine.audit.service.AuditService;
import cm.ftg.tontine.auth.repository.UserRepository;
import cm.ftg.tontine.common.enums.UserRole;
import cm.ftg.tontine.common.exception.ApiException;
import cm.ftg.tontine.common.exception.ResourceNotFoundException;
import cm.ftg.tontine.integration.messaging.email.EmailSender;
import cm.ftg.tontine.integration.messaging.sms.SmsSender;
import cm.ftg.tontine.member.entity.Member;
import cm.ftg.tontine.member.repository.MemberRepository;
import cm.ftg.tontine.notification.enums.NotificationCategory;
import cm.ftg.tontine.notification.enums.NotificationKind;
import cm.ftg.tontine.notification.service.NotificationService;
import cm.ftg.tontine.president.presidencytransfer.dto.PresidencyTransferDto;
import cm.ftg.tontine.president.presidencytransfer.entity.PresidencyTransfer;
import cm.ftg.tontine.president.presidencytransfer.enums.PresidencyTransferStatus;
import cm.ftg.tontine.president.presidencytransfer.repository.PresidencyTransferRepository;
import cm.ftg.tontine.realtime.RealtimeEventPublisher;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Acceptation du transfert : swap atomique des roles PRESIDENT.
 *
 * <p>L'atomicite repose sur le {@code @Version} de l'entite (verrouillage optimiste).
 * En cas de concurrence (deux accepts simultanes), un seul commit reussit ; l'autre leve
 * une {@link ConcurrencyFailureException}, traduite en {@code 409 TRANSFER_CONFLICT}.
 * On reste en {@link Isolation#READ_COMMITTED} : sous H2 (tests) un niveau superieur
 * (REPEATABLE_READ) abandonne les deux transactions en cas de contention ecriture-ecriture,
 * alors que READ_COMMITTED + {@code @Version} garantit l'exactement-une-reussite sur H2
 * comme sur PostgreSQL.</p>
 *
 * <p>Apres acceptation, l'ancien et le nouveau President doivent rappeler {@code /auth/refresh}
 * pour obtenir un JWT portant les roles a jour.</p>
 */
@Service
public class PresidencyAcceptService {

    private static final Logger log = LoggerFactory.getLogger(PresidencyAcceptService.class);
    private static final HttpStatus UNPROCESSABLE = HttpStatus.valueOf(422);

    private final PresidencyTransferRepository transferRepository;
    private final MemberRepository memberRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final RealtimeEventPublisher realtime;
    private final NotificationService notificationService;
    private final SmsSender smsSender;
    private final EmailSender emailSender;
    private final ObjectProvider<PresidencyAcceptService> self;
    private final String frontendBaseUrl;

    public PresidencyAcceptService(PresidencyTransferRepository transferRepository,
                                   MemberRepository memberRepository,
                                   UserRepository userRepository,
                                   AuditService auditService,
                                   RealtimeEventPublisher realtime,
                                   NotificationService notificationService,
                                   SmsSender smsSender,
                                   EmailSender emailSender,
                                   ObjectProvider<PresidencyAcceptService> self,
                                   @Value("${app.frontend.base-url:https://app.tontine-connect.cm}") String frontendBaseUrl) {
        this.transferRepository = transferRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.realtime = realtime;
        this.notificationService = notificationService;
        this.smsSender = smsSender;
        this.emailSender = emailSender;
        this.self = self;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    /**
     * Point d'entree public : delegue a la methode transactionnelle via le proxy
     * (ObjectProvider) afin de pouvoir intercepter le conflit optimiste leve au commit.
     */
    public PresidencyTransferDto accept(UUID id, UUID userId) {
        try {
            return self.getObject().doAccept(id, userId);
        } catch (ConcurrencyFailureException ex) {
            throw new ApiException("TRANSFER_CONFLICT",
                    "Conflit de concurrence sur ce transfert, veuillez reessayer", HttpStatus.CONFLICT);
        }
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public PresidencyTransferDto doAccept(UUID id, UUID userId) {
        PresidencyTransfer transfer = transferRepository.findById(id)
                .orElseThrow(() -> new ApiException("PRESIDENCY_TRANSFER_NOT_FOUND",
                        "Transfert introuvable", HttpStatus.NOT_FOUND));
        if (transfer.getTargetUserId() == null || !transfer.getTargetUserId().equals(userId)) {
            throw new ApiException("FORBIDDEN",
                    "Vous n'etes pas le destinataire de cette proposition", HttpStatus.FORBIDDEN);
        }
        if (transfer.getStatus() != PresidencyTransferStatus.PENDING) {
            throw new ApiException("TRANSFER_NOT_PENDING",
                    "Ce transfert n'est plus en attente", UNPROCESSABLE);
        }
        if (transfer.getExpiresAt().isBefore(Instant.now())) {
            // Persiste la bascule EXPIRED dans une transaction independante : le throw
            // ci-dessous annulerait sinon l'ecriture (rollback de la transaction courante).
            self.getObject().markExpired(transfer.getId());
            throw new ApiException("TRANSFER_EXPIRED",
                    "Cette proposition a expire", UNPROCESSABLE);
        }
        UUID tontineId = transfer.getTontineId();

        Member oldPres = memberRepository.findByTontineIdAndRole(tontineId, UserRole.PRESIDENT).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Aucun President actuel"));
        Member newPres = memberRepository.findByIdAndTontineId(transfer.getTargetMemberId(), tontineId)
                .orElseThrow(() -> new ResourceNotFoundException("Member", transfer.getTargetMemberId()));

        // Swap atomique des roles.
        oldPres.getRoles().remove(UserRole.PRESIDENT);
        if (!oldPres.getRoles().contains(UserRole.MEMBER)) {
            oldPres.getRoles().add(UserRole.MEMBER);
        }
        newPres.getRoles().add(UserRole.PRESIDENT);
        memberRepository.save(oldPres);
        memberRepository.save(newPres);

        transfer.setStatus(PresidencyTransferStatus.ACCEPTED);
        transfer.setAcceptedAt(Instant.now());
        PresidencyTransfer saved = transferRepository.save(transfer);

        auditService.record(userId, "PRESIDENCY_TRANSFER_ACCEPT", "PresidencyTransfer",
                saved.getId().toString(), tontineId,
                String.format("{\"from\":\"%s\",\"to\":\"%s\"}", oldPres.getUserId(), newPres.getUserId()));

        PresidencyTransferDto dto = PresidencyTransferDto.from(saved);
        realtime.toTontine(tontineId, "presidency-transfer.accepted", dto);
        notifyAfterAccept(saved, oldPres);
        return dto;
    }

    /** Bascule un transfert PENDING en EXPIRED dans une transaction independante. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markExpired(UUID id) {
        transferRepository.findById(id).ifPresent(t -> {
            if (t.getStatus() == PresidencyTransferStatus.PENDING) {
                t.setStatus(PresidencyTransferStatus.EXPIRED);
                transferRepository.save(t);
            }
        });
    }

    private void notifyAfterAccept(PresidencyTransfer transfer, Member oldPres) {
        String title = "Transfert de presidence effectue";
        String message = transfer.getTargetMemberFullName()
                + " est desormais President de la tontine. " + transfer.getInitiatedByFullName()
                + " redevient membre.";
        String link = frontendBaseUrl + "/dashboard";

        // In-app pour tous les membres de la tontine.
        for (Member m : memberRepository.findByTontineId(transfer.getTontineId())) {
            notificationService.publish(m.getUserId(), transfer.getTontineId(), NotificationKind.INFO,
                    NotificationCategory.GENERAL, title, message, link);
        }
        // SMS + e-mail pour l'ancien President.
        if (oldPres.getUserId() != null) {
            userRepository.findById(oldPres.getUserId()).ifPresent(u -> {
                if (u.getPhone() != null && !u.getPhone().isBlank()) {
                    try {
                        smsSender.send(u.getPhone(), message);
                    } catch (RuntimeException ex) {
                        log.warn("[PresidencyTransfer] echec SMS ancien President : {}", ex.getMessage());
                    }
                }
                if (u.getEmail() != null && !u.getEmail().isBlank()) {
                    try {
                        emailSender.send(u.getEmail(), title, "<p>" + message + "</p>");
                    } catch (RuntimeException ex) {
                        log.warn("[PresidencyTransfer] echec e-mail ancien President : {}", ex.getMessage());
                    }
                }
            });
        }
    }
}
