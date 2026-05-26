package cm.ftg.tontine.integration.messaging.email;

public interface EmailSender {

    void send(String toAddress, String subject, String htmlBody);
}
