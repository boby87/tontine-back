package cm.ftg.tontine.president.membership.invitation.service;

import cm.ftg.tontine.common.enums.UserRole;
import cm.ftg.tontine.integration.messaging.email.EmailSender;
import cm.ftg.tontine.integration.messaging.sms.SmsSender;
import cm.ftg.tontine.president.membership.invitation.entity.MembershipInvitation;
import cm.ftg.tontine.president.membership.invitation.enums.InvitationChannel;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Envoie l'invitation sur les canaux demandes (SMS, e-mail, WhatsApp en fallback SMS).
 * Un echec sur un canal est logge sans interrompre les autres ; la methode renvoie
 * {@code true} si au moins un canal a abouti.
 */
@Component
public class InvitationDispatcher {

    private static final Logger log = LoggerFactory.getLogger(InvitationDispatcher.class);
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneOffset.UTC);

    private final SmsSender smsSender;
    private final EmailSender emailSender;
    private final String frontendBaseUrl;

    public InvitationDispatcher(SmsSender smsSender,
                                EmailSender emailSender,
                                @Value("${app.frontend.base-url:https://app.tontine-connect.cm}") String frontendBaseUrl) {
        this.smsSender = smsSender;
        this.emailSender = emailSender;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    public boolean dispatch(MembershipInvitation inv, String tontineName) {
        String acceptUrl = frontendBaseUrl + "/auth/invitations/" + inv.getToken() + "/accept";
        Set<InvitationChannel> channels = parseChannels(inv.getChannels());
        boolean anySuccess = false;
        for (InvitationChannel channel : channels) {
            switch (channel) {
                case SMS -> anySuccess |= sendSms(inv, tontineName, acceptUrl);
                case WHATSAPP -> {
                    // TODO whatsapp : pas d'API WhatsApp Business pour le moment, fallback SMS.
                    log.info("[Invitation {}] canal WHATSAPP non implemente, fallback SMS", inv.getId());
                    anySuccess |= sendSms(inv, tontineName, acceptUrl);
                }
                case EMAIL -> anySuccess |= sendEmail(inv, tontineName, acceptUrl);
            }
        }
        return anySuccess;
    }

    private boolean sendSms(MembershipInvitation inv, String tontineName, String acceptUrl) {
        try {
            smsSender.send(inv.getCandidatePhone(), buildSmsBody(inv, tontineName, acceptUrl));
            return true;
        } catch (RuntimeException ex) {
            log.warn("[Invitation {}] echec envoi SMS : {}", inv.getId(), ex.getMessage());
            return false;
        }
    }

    private boolean sendEmail(MembershipInvitation inv, String tontineName, String acceptUrl) {
        if (inv.getCandidateEmail() == null || inv.getCandidateEmail().isBlank()) {
            log.info("[Invitation {}] canal EMAIL demande mais aucune adresse fournie", inv.getId());
            return false;
        }
        try {
            emailSender.send(inv.getCandidateEmail(),
                    "Invitation a rejoindre la tontine \"" + tontineName + "\"",
                    buildEmailBody(inv, tontineName, acceptUrl));
            return true;
        } catch (RuntimeException ex) {
            log.warn("[Invitation {}] echec envoi e-mail : {}", inv.getId(), ex.getMessage());
            return false;
        }
    }

    private String buildSmsBody(MembershipInvitation inv, String tontineName, String acceptUrl) {
        return "Bonjour %s, vous etes invite(e) a rejoindre la tontine \"%s\" comme %s. Acceptez : %s"
                .formatted(inv.getCandidateFullName(), tontineName, roleLabel(inv.getProposedRole()), acceptUrl);
    }

    private String buildEmailBody(MembershipInvitation inv, String tontineName, String acceptUrl) {
        String customMessage = (inv.getMessage() == null || inv.getMessage().isBlank())
                ? ""
                : "<p><em>" + escape(inv.getMessage()) + "</em></p>";
        return """
                <div>
                  <h2>Invitation a la tontine "%s"</h2>
                  <p>Bonjour %s,</p>
                  <p>%s vous invite a rejoindre la tontine <strong>"%s"</strong> en tant que <strong>%s</strong>.</p>
                  %s
                  <p><a href="%s">Accepter l'invitation</a></p>
                  <p>Cette invitation expire le %s.</p>
                </div>
                """.formatted(
                escape(tontineName),
                escape(inv.getCandidateFullName()),
                escape(inv.getInvitedByFullName()),
                escape(tontineName),
                roleLabel(inv.getProposedRole()),
                customMessage,
                acceptUrl,
                DATE_FMT.format(inv.getExpiresAt()));
    }

    private Set<InvitationChannel> parseChannels(String csv) {
        return Arrays.stream(csv.split(","))
                .map(String::trim).filter(s -> !s.isEmpty())
                .map(InvitationChannel::valueOf)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    private String roleLabel(UserRole role) {
        return switch (role) {
            case PRESIDENT -> "President";
            case VICE_PRESIDENT -> "Vice-President";
            case SECRETARY -> "Secretaire";
            case SECRETARY_ADJOINT -> "Secretaire Adjoint";
            case TREASURER -> "Tresorier";
            case TREASURER_ADJOINT -> "Tresorier Adjoint";
            case CENSOR -> "Censeur";
            case AUDITOR -> "Commissaire aux comptes";
            case ADMIN -> "Administrateur";
            case MEMBER -> "Membre";
        };
    }

    private String escape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
