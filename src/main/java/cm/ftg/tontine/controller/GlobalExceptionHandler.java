package cm.ftg.tontine.controller;

import cm.ftg.tontine.exception.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthentificationEchoueeException.class)
    public ResponseEntity<ErrorResponse> handleAuthentificationEchouee(AuthentificationEchoueeException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(new ErrorResponse("AUTH_FAILED", ex.getMessage()));
    }

    @ExceptionHandler(MembreIntrouvableException.class)
    public ResponseEntity<ErrorResponse> handleMembreIntrouvable(MembreIntrouvableException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse("MEMBRE_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(MembreInactifException.class)
    public ResponseEntity<ErrorResponse> handleMembreInactif(MembreInactifException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(new ErrorResponse("MEMBRE_INACTIF", ex.getMessage()));
    }

    @ExceptionHandler(SeanceNonOuverteException.class)
    public ResponseEntity<ErrorResponse> handleSeanceNonOuverte(SeanceNonOuverteException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse("SEANCE_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(DoubleCotisationException.class)
    public ResponseEntity<ErrorResponse> handleDoubleCotisation(DoubleCotisationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(new ErrorResponse("DUPLICATE_CONTRIBUTION", ex.getMessage()));
    }

    @ExceptionHandler(MontantInvalideException.class)
    public ResponseEntity<ErrorResponse> handleMontantInvalide(MontantInvalideException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse("INVALID_AMOUNT", ex.getMessage()));
    }

    @ExceptionHandler(TontineIntrouvableException.class)
    public ResponseEntity<ErrorResponse> handleTontineIntrouvable(TontineIntrouvableException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse("TONTINE_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(UtilisateurIntrouvableException.class)
    public ResponseEntity<ErrorResponse> handleUtilisateurIntrouvable(UtilisateurIntrouvableException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse("USER_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(NomTontineDejaExistantException.class)
    public ResponseEntity<ErrorResponse> handleNomTontineDejaExistant(NomTontineDejaExistantException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(new ErrorResponse("DUPLICATE_TONTINE_NAME", ex.getMessage()));
    }

    @ExceptionHandler(DateDansLePasseException.class)
    public ResponseEntity<ErrorResponse> handleDateDansLePasse(DateDansLePasseException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse("DATE_IN_PAST", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse("INVALID_ARGUMENT", ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(new ErrorResponse("INVALID_STATE", ex.getMessage()));
    }

    @ExceptionHandler(EmailDejaExistantException.class)
    public ResponseEntity<ErrorResponse> handleEmailDejaExistant(EmailDejaExistantException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(new ErrorResponse("DUPLICATE_EMAIL", ex.getMessage()));
    }

    @ExceptionHandler(TelephoneDejaExistantException.class)
    public ResponseEntity<ErrorResponse> handleTelephoneDejaExistant(TelephoneDejaExistantException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(new ErrorResponse("DUPLICATE_PHONE", ex.getMessage()));
    }

    @ExceptionHandler(SessionNonEnCoursException.class)
    public ResponseEntity<ErrorResponse> handleSessionNonEnCours(SessionNonEnCoursException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(new ErrorResponse("SESSION_NOT_IN_PROGRESS", ex.getMessage()));
    }

    @ExceptionHandler(DoubleSanctionException.class)
    public ResponseEntity<ErrorResponse> handleDoubleSanction(DoubleSanctionException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(new ErrorResponse("DUPLICATE_SANCTION", ex.getMessage()));
    }

    @ExceptionHandler(MembreTontineIntrouvableException.class)
    public ResponseEntity<ErrorResponse> handleMembreTontineIntrouvable(MembreTontineIntrouvableException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse("TONTINE_MEMBER_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(MembreDejaExistantException.class)
    public ResponseEntity<ErrorResponse> handleMembreDejaExistant(MembreDejaExistantException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(new ErrorResponse("DUPLICATE_MEMBER", ex.getMessage()));
    }

    @ExceptionHandler(AccesNonAutoriseException.class)
    public ResponseEntity<ErrorResponse> handleAccesNonAutorise(AccesNonAutoriseException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(new ErrorResponse("ACCESS_DENIED", ex.getMessage()));
    }

    @ExceptionHandler(InvitationInvalideException.class)
    public ResponseEntity<ErrorResponse> handleInvitationInvalide(InvitationInvalideException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse("INVALID_INVITATION", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest()
            .body(new ErrorResponse("VALIDATION_ERROR", message));
    }
}
