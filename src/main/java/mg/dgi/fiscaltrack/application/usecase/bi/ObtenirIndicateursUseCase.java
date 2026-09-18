package mg.dgi.fiscaltrack.application.usecase.bi;

import mg.dgi.fiscaltrack.application.port.out.CompteCourantFiscalRepositoryPort;
import mg.dgi.fiscaltrack.application.port.out.ContribuableRepositoryPort;
import mg.dgi.fiscaltrack.application.port.out.ObligationFiscaleRepositoryPort;
import mg.dgi.fiscaltrack.domain.enums.StatutDeclaration;
import mg.dgi.fiscaltrack.domain.model.CompteCourantFiscal;
import mg.dgi.fiscaltrack.domain.model.ObligationFiscale;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ObtenirIndicateursUseCase {

    private final ContribuableRepositoryPort contribuableRepositoryPort;
    private final ObligationFiscaleRepositoryPort obligationRepositoryPort;
    private final CompteCourantFiscalRepositoryPort compteRepositoryPort;

    public ObtenirIndicateursUseCase(ContribuableRepositoryPort contribuableRepositoryPort,
                                      ObligationFiscaleRepositoryPort obligationRepositoryPort,
                                      CompteCourantFiscalRepositoryPort compteRepositoryPort) {
        this.contribuableRepositoryPort = contribuableRepositoryPort;
        this.obligationRepositoryPort = obligationRepositoryPort;
        this.compteRepositoryPort = compteRepositoryPort;
    }

    /**
     * Retourne les indicateurs cles du tableau de bord :
     * nombre de contribuables, obligations par statut, montants recouvres.
     */
    public Map<String, Object> execute() {
        Map<String, Object> indicateurs = new HashMap<>();

        long nbContribuables = contribuableRepositoryPort.findAll().size();
        indicateurs.put("nombreContribuables", nbContribuables);

        List<ObligationFiscale> obligations = obligationRepositoryPort.findAll();
        long enAttente = obligations.stream()
                .filter(o -> o.getStatutDeclaration() == StatutDeclaration.ATTENTE)
                .count();
        long depose = obligations.stream()
                .filter(o -> o.getStatutDeclaration() == StatutDeclaration.DEPOSE)
                .count();
        long retard = obligations.stream()
                .filter(o -> o.getStatutDeclaration() == StatutDeclaration.RETARD)
                .count();

        indicateurs.put("obligationsEnAttente", enAttente);
        indicateurs.put("obligationsDeposees", depose);
        indicateurs.put("obligationsEnRetard", retard);
        indicateurs.put("totalObligations", obligations.size());

        if (obligations.size() > 0) {
            double tauxConformite = 100.0 * depose / obligations.size();
            indicateurs.put("tauxConformite", Math.round(tauxConformite * 100.0) / 100.0);
        } else {
            indicateurs.put("tauxConformite", 0.0);
        }

        List<CompteCourantFiscal> comptes = compteRepositoryPort.findAll();
        BigDecimal totalDu = comptes.stream()
                .map(CompteCourantFiscal::getMontantTotalDu)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPaye = comptes.stream()
                .map(CompteCourantFiscal::getMontantPaye)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalARecouvrer = comptes.stream()
                .map(CompteCourantFiscal::getResteARecouvrer)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        indicateurs.put("montantTotalDu", totalDu);
        indicateurs.put("montantTotalPaye", totalPaye);
        indicateurs.put("montantTotalARecouvrer", totalARecouvrer);

        return indicateurs;
    }
}
