package cm.ftg.tontine.exception;

public class MembreIntrouvableException extends RuntimeException {
    public MembreIntrouvableException(Long membreId, Long tontineId) {
        super("Membre %d introuvable dans la tontine %d".formatted(membreId, tontineId));
    }
}
