package cm.ftg.tontine.exception;

public class EmailDejaExistantException extends RuntimeException {
    public EmailDejaExistantException(String email) {
        super("Un compte existe déjà avec l'email : %s".formatted(email));
    }
}
