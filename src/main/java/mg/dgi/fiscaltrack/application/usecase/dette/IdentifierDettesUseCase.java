package mg.dgi.fiscaltrack.application.usecase.dette;

import mg.dgi.fiscaltrack.application.port.out.CompteCourantFiscalRepositoryPort;
import mg.dgi.fiscaltrack.domain.model.CompteCourantFiscal;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class IdentifierDettesUseCase {

    private final CompteCourantFiscalRepositoryPort compteRepositoryPort;

    public IdentifierDettesUseCase(CompteCourantFiscalRepositoryPort compteRepositoryPort) {
        this.compteRepositoryPort = compteRepositoryPort;
    }

    /**
     * Retourne toutes les lignes de compte courant fiscal dont le reste
     * a recouvrer est strictement positif.
     */
    public List<CompteCourantFiscal> execute() {
        return compteRepositoryPort.findNonSoldes().stream()
                .filter(c -> {
                    BigDecimal reste = c.getResteARecouvrer();
                    if (reste == null) {
                        reste = c.getMontantPrincipal()
                                .add(c.getMontantPenalites())
                                .subtract(c.getMontantPaye());
                    }
                    return reste.compareTo(BigDecimal.ZERO) > 0;
                })
                .toList();
    }
}
