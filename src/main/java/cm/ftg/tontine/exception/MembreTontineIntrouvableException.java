package cm.ftg.tontine.exception;

public class MembreTontineIntrouvableException extends RuntimeException {
    public MembreTontineIntrouvableException(String memberId) {
        super("Membre tontine introuvable : %s".formatted(memberId));
    }
}
