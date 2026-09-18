package mg.dgi.fiscaltrack.application.usecase.paiement;

import mg.dgi.fiscaltrack.application.port.out.CompteCourantFiscalRepositoryPort;
import mg.dgi.fiscaltrack.domain.model.CompteCourantFiscal;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class CalculerResteARecouvrerUseCase {

    private final CompteCourantFiscalRepositoryPort compteRepositoryPort;

    public CalculerResteARecouvrerUseCase(CompteCourantFiscalRepositoryPort compteRepositoryPort) {
        this.compteRepositoryPort = compteRepositoryPort;
    }

    /**
     * Le reste a recouvrer est normalement calcule par PostgreSQL
     * (colonne GENERATED). Cette methode le recalcule cote Java pour
     * verification ou pour les cas ou le compte n'est pas encore persiste.
     */
    public BigDecimal execute(Long idCompte) {
        CompteCourantFiscal compte = compteRepositoryPort.findById(idCompte)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Compte courant fiscal introuvable : " + idCompte));

        if (compte.getResteARecouvrer() != null) {
            return compte.getResteARecouvrer();
        }
        return compte.getMontantPrincipal()
                .add(compte.getMontantPenalites())
                .subtract(compte.getMontantPaye());
    }
}
