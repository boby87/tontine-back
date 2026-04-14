package cm.ftg.tontine.exception;

public class MembreInactifException extends RuntimeException {
    public MembreInactifException(Long membreId) {
        super("Le membre %d n'est pas actif".formatted(membreId));
    }

    public MembreInactifException(String membreId) {
        super("Le membre %s n'est pas actif".formatted(membreId));
    }
}
