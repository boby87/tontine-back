package cm.ftg.tontine.exception;

public class UtilisateurIntrouvableException extends RuntimeException {
    public UtilisateurIntrouvableException(String userId) {
        super("Utilisateur %s introuvable".formatted(userId));
    }
}
