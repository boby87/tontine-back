package cm.ftg.tontine.integration.messaging.email;

import cm.ftg.tontine.integration.messaging.config.MessagingProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.messaging.email.provider", havingValue = "smtp")
public class SmtpEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailSender.class);

    private final JavaMailSender mailSender;
    private final MessagingProperties properties;

    public SmtpEmailSender(JavaMailSender mailSender, MessagingProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    @Override
    public void send(String toAddress, String subject, String htmlBody) {
        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, "UTF-8");
            helper.setTo(toAddress);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            helper.setFrom(new InternetAddress(
                    properties.getEmail().getFromAddress(),
                    properties.getEmail().getFromName(),
                    "UTF-8"));
            mailSender.send(mime);
        } catch (MessagingException | UnsupportedEncodingException | MailException e) {
            log.warn("Echec envoi email subject=\"{}\" : {}", subject, e.getMessage());
            throw new IllegalStateException("Envoi email impossible", e);
        }
    }
}
