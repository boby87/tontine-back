package cm.ftg.tontine.exception;

public class SessionNonEnCoursException extends RuntimeException {
    public SessionNonEnCoursException(Long sessionId) {
        super("La séance %d n'est pas en cours (statut IN_PROGRESS requis)".formatted(sessionId));
    }
}
