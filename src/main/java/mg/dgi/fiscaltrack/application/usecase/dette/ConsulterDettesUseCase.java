package mg.dgi.fiscaltrack.application.usecase.dette;

import mg.dgi.fiscaltrack.application.port.out.CompteCourantFiscalRepositoryPort;
import mg.dgi.fiscaltrack.domain.model.CompteCourantFiscal;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ConsulterDettesUseCase {

    private final CompteCourantFiscalRepositoryPort compteRepositoryPort;

    public ConsulterDettesUseCase(CompteCourantFiscalRepositoryPort compteRepositoryPort) {
        this.compteRepositoryPort = compteRepositoryPort;
    }

    public List<CompteCourantFiscal> parContribuable(String nif) {
        return compteRepositoryPort.findByNif(nif).stream()
                .filter(this::aUneDette)
                .toList();
    }

    public BigDecimal totalDetteContribuable(String nif) {
        return compteRepositoryPort.findByNif(nif).stream()
                .filter(this::aUneDette)
                .map(this::calculerReste)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal totalDetteGlobale() {
        return compteRepositoryPort.findNonSoldes().stream()
                .filter(this::aUneDette)
                .map(this::calculerReste)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private boolean aUneDette(CompteCourantFiscal c) {
        return calculerReste(c).compareTo(BigDecimal.ZERO) > 0;
    }

    private BigDecimal calculerReste(CompteCourantFiscal c) {
        if (c.getResteARecouvrer() != null) {
            return c.getResteARecouvrer();
        }
        return c.getMontantPrincipal()
                .add(c.getMontantPenalites())
                .subtract(c.getMontantPaye());
    }
}
