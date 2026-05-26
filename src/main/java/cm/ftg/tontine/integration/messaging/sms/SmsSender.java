package cm.ftg.tontine.integration.messaging.sms;

public interface SmsSender {

    /**
     * Envoie un SMS. L'implementation doit etre idempotente vis-a-vis des erreurs
     * (un echec leve une exception, pas d'effet de bord partiel attendu).
     */
    void send(String toPhone, String body);
}
