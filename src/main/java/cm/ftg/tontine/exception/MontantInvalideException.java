package cm.ftg.tontine.exception;

import java.math.BigDecimal;

public class MontantInvalideException extends RuntimeException {
    public MontantInvalideException(BigDecimal attendu, BigDecimal recu) {
        super("Montant invalide : attendu %s, reçu %s".formatted(attendu.toPlainString(), recu.toPlainString()));
    }
}
