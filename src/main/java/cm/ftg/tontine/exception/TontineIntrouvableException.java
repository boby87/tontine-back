package cm.ftg.tontine.exception;

public class TontineIntrouvableException extends RuntimeException {
    public TontineIntrouvableException(Long tontineId) {
        super("Tontine %d introuvable".formatted(tontineId));
    }
}
