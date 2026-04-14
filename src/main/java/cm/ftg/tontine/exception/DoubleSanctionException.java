package cm.ftg.tontine.exception;

public class DoubleSanctionException extends RuntimeException {
    public DoubleSanctionException(String cleIdempotence) {
        super("Une sanction existe déjà avec la clé d'idempotence : %s".formatted(cleIdempotence));
    }
}
