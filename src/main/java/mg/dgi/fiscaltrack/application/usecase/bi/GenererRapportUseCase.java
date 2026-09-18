package mg.dgi.fiscaltrack.application.usecase.bi;

import mg.dgi.fiscaltrack.application.port.out.CompteCourantFiscalRepositoryPort;
import mg.dgi.fiscaltrack.domain.model.CompteCourantFiscal;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Service
public class GenererRapportUseCase {

    private final CompteCourantFiscalRepositoryPort compteRepositoryPort;

    public GenererRapportUseCase(CompteCourantFiscalRepositoryPort compteRepositoryPort) {
        this.compteRepositoryPort = compteRepositoryPort;
    }

    /**
     * Retourne le top 10 des comptes courants fiscaux par reste a recouvrer
     * decroissant. Utilise par le Directeur pour prioriser les actions de recouvrement.
     */
    public List<CompteCourantFiscal> top10RestesARecouvrer() {
        return compteRepositoryPort.findAll().stream()
                .filter(c -> c.getResteARecouvrer() != null
                        && c.getResteARecouvrer().compareTo(BigDecimal.ZERO) > 0)
                .sorted(Comparator.comparing(CompteCourantFiscal::getResteARecouvrer).reversed())
                .limit(10)
                .toList();
    }
}
