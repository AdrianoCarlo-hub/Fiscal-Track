package mg.dgi.fiscaltrack.interfaces.exception;

import jakarta.servlet.http.HttpServletRequest;
import mg.dgi.fiscaltrack.domain.exception.AccesNonAutoriseException;
import mg.dgi.fiscaltrack.domain.exception.ContribuableIntrouvableException;
import mg.dgi.fiscaltrack.domain.exception.DeclarationDejaExistanteException;
import mg.dgi.fiscaltrack.domain.exception.ObligationIntrouvableException;
import mg.dgi.fiscaltrack.domain.exception.PaiementInvalideException;
import mg.dgi.fiscaltrack.interfaces.dto.response.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.OffsetDateTime;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ContribuableIntrouvableException.class)
    public ResponseEntity<ErrorResponse> handleContribuableIntrouvable(
            ContribuableIntrouvableException e, HttpServletRequest request) {
        return construire(HttpStatus.NOT_FOUND, "NOT_FOUND", e.getMessage(), request);
    }

    @ExceptionHandler(ObligationIntrouvableException.class)
    public ResponseEntity<ErrorResponse> handleObligationIntrouvable(
            ObligationIntrouvableException e, HttpServletRequest request) {
        return construire(HttpStatus.NOT_FOUND, "NOT_FOUND", e.getMessage(), request);
    }

    @ExceptionHandler(DeclarationDejaExistanteException.class)
    public ResponseEntity<ErrorResponse> handleDeclarationDejaExistante(
            DeclarationDejaExistanteException e, HttpServletRequest request) {
        return construire(HttpStatus.CONFLICT, "CONFLICT", e.getMessage(), request);
    }

    @ExceptionHandler(PaiementInvalideException.class)
    public ResponseEntity<ErrorResponse> handlePaiementInvalide(
            PaiementInvalideException e, HttpServletRequest request) {
        return construire(HttpStatus.BAD_REQUEST, "BAD_REQUEST", e.getMessage(), request);
    }

    @ExceptionHandler(AccesNonAutoriseException.class)
    public ResponseEntity<ErrorResponse> handleAccesNonAutorise(
            AccesNonAutoriseException e, HttpServletRequest request) {
        return construire(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", e.getMessage(), request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException e, HttpServletRequest request) {
        return construire(HttpStatus.FORBIDDEN, "FORBIDDEN",
                "Acces refuse : vous n'avez pas les droits necessaires", request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(
            AuthenticationException e, HttpServletRequest request) {
        return construire(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED",
                "Authentification echouee", request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException e, HttpServletRequest request) {
        StringBuilder message = new StringBuilder();
        for (FieldError erreur : e.getBindingResult().getFieldErrors()) {
            if (message.length() > 0) {
                message.append(" ; ");
            }
            message.append(erreur.getField()).append(" : ").append(erreur.getDefaultMessage());
        }
        return construire(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                message.toString(), request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException e, HttpServletRequest request) {
        return construire(HttpStatus.BAD_REQUEST, "BAD_REQUEST", e.getMessage(), request);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntime(
            RuntimeException e, HttpServletRequest request) {
        return construire(HttpStatus.BAD_REQUEST, "BAD_REQUEST",
                e.getMessage() != null ? e.getMessage() : "Erreur inattendue", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(
            Exception e, HttpServletRequest request) {
        return construire(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR",
                "Une erreur interne est survenue", request);
    }

    private ResponseEntity<ErrorResponse> construire(HttpStatus status, String error,
                                                      String message, HttpServletRequest request) {
        ErrorResponse response = ErrorResponse.builder()
                .timestamp(OffsetDateTime.now().toString())
                .status(status.value())
                .error(error)
                .message(message)
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(status).body(response);
    }
}
