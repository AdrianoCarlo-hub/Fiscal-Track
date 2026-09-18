package mg.dgi.fiscaltrack.domain.exception;

public class DeclarationDejaExistanteException extends RuntimeException {

    public DeclarationDejaExistanteException(Long idObligationFiscale) {
        super("Une declaration existe deja pour l'obligation fiscale : " + idObligationFiscale);
    }
}
