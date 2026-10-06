package mg.dgi.fiscaltrack.domain.exception;

/**
 * Levee lorsque le moteur de calcul fiscal recoit un code impot
 * qui n'est pas supporte par le dispatcher.
 */
public class TypeImpotNonSupporteException extends RuntimeException {

    public TypeImpotNonSupporteException(String codeImpot) {
        super("Type d'impot non supporte par le moteur de calcul : " + codeImpot);
    }
}
