package cm.ftg.tontine.exception;

public class NomTontineDejaExistantException extends RuntimeException {
    public NomTontineDejaExistantException(String nom) {
        super("Une tontine avec le nom '%s' existe déjà".formatted(nom));
    }
}
