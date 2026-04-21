package cm.ftg.tontine.exception;

public class MembreDejaExistantException extends RuntimeException {
    public MembreDejaExistantException(Long tontineId, String userId) {
        super("L'utilisateur %s est déjà membre de la tontine %d".formatted(userId, tontineId));
    }
}

