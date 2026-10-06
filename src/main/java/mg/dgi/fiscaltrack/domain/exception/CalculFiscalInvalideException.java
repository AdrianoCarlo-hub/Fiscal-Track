package mg.dgi.fiscaltrack.domain.exception;

/**
 * Levee lorsqu'un calcul fiscal ne peut pas etre effectue
 * en raison de donnees invalides ou incoherentes.
 */
public class CalculFiscalInvalideException extends RuntimeException {

    public CalculFiscalInvalideException(String message) {
        super(message);
    }
}
