package cm.ftg.tontine.exception;

public class DoubleCotisationException extends RuntimeException {
    public DoubleCotisationException(Long membreId, Long seanceId) {
        super("Une cotisation existe déjà pour le membre %d à la séance %d".formatted(membreId, seanceId));
    }
}
