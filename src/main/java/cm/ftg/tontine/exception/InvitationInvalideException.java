package cm.ftg.tontine.exception;

/**
 * Levée lorsqu'un token d'invitation est invalide, expiré ou épuisé.
 */
public class InvitationInvalideException extends RuntimeException {
    public InvitationInvalideException(String raison) {
        super("Token d'invitation invalide : " + raison);
    }
}

