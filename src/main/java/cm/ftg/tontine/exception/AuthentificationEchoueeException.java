package cm.ftg.tontine.exception;

/**
 * Exception levée lorsque l'authentification échoue (email inconnu ou mot de passe incorrect).
 * Le message ne doit JAMAIS révéler lequel des deux est erroné (security-rules §4).
 */
public class AuthentificationEchoueeException extends RuntimeException {

    public AuthentificationEchoueeException() {
        super("Email ou mot de passe incorrect");
    }
}

