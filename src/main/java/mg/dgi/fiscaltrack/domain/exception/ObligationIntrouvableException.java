package mg.dgi.fiscaltrack.domain.exception;

public class ObligationIntrouvableException extends RuntimeException {

    public ObligationIntrouvableException(Long idObligationFiscale) {
        super("Obligation fiscale introuvable avec l'identifiant : " + idObligationFiscale);
    }
}
