package mg.dgi.fiscaltrack.application.usecase.obligation;

import mg.dgi.fiscaltrack.application.port.out.ObligationFiscaleRepositoryPort;
import mg.dgi.fiscaltrack.domain.exception.ObligationIntrouvableException;
import mg.dgi.fiscaltrack.domain.model.ObligationFiscale;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ConsulterObligationsUseCase {

    private final ObligationFiscaleRepositoryPort repositoryPort;

    public ConsulterObligationsUseCase(ObligationFiscaleRepositoryPort repositoryPort) {
        this.repositoryPort = repositoryPort;
    }

    public ObligationFiscale parId(Long id) {
        return repositoryPort.findById(id)
                .orElseThrow(() -> new ObligationIntrouvableException(id));
    }

    public List<ObligationFiscale> toutes() {
        return repositoryPort.findAll();
    }

    public List<ObligationFiscale> parContribuable(String nif) {
        return repositoryPort.findByNif(nif);
    }

    public List<ObligationFiscale> parStatut(String statutDeclaration) {
        return repositoryPort.findByStatut(statutDeclaration);
    }

    public List<ObligationFiscale> parAgent(String idAgent) {
        return repositoryPort.findByAgent(idAgent);
    }
}
