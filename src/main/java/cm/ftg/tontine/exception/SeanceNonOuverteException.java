package cm.ftg.tontine.exception;

public class SeanceNonOuverteException extends RuntimeException {
    public SeanceNonOuverteException(Long seanceId) {
        super("La séance %d n'est pas ouverte aux cotisations".formatted(seanceId));
    }
}
