package cm.ftg.tontine.exception;

public class TelephoneDejaExistantException extends RuntimeException {
    public TelephoneDejaExistantException(String phone) {
        super("Un compte existe déjà avec le téléphone : %s".formatted(phone));
    }
}
