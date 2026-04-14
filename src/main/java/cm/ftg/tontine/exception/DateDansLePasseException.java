package cm.ftg.tontine.exception;

import java.time.LocalDate;

public class DateDansLePasseException extends RuntimeException {
    public DateDansLePasseException(LocalDate date) {
        super("La date %s est dans le passé".formatted(date));
    }
}
