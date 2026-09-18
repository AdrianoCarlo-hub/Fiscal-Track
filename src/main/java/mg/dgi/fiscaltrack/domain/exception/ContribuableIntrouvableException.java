package mg.dgi.fiscaltrack.domain.exception;

public class ContribuableIntrouvableException extends RuntimeException {

    public ContribuableIntrouvableException(String nif) {
        super("Contribuable introuvable avec le NIF : " + nif);
    }
}
