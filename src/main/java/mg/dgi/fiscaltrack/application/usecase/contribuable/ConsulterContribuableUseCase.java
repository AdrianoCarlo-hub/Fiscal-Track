package mg.dgi.fiscaltrack.application.usecase.contribuable;

import mg.dgi.fiscaltrack.application.port.out.ContribuableRepositoryPort;
import mg.dgi.fiscaltrack.domain.exception.ContribuableIntrouvableException;
import mg.dgi.fiscaltrack.domain.model.Contribuable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ConsulterContribuableUseCase {

    private final ContribuableRepositoryPort repositoryPort;

    public ConsulterContribuableUseCase(ContribuableRepositoryPort repositoryPort) {
        this.repositoryPort = repositoryPort;
    }

    public Contribuable parNif(String nif) {
        return repositoryPort.findByNif(nif)
                .orElseThrow(() -> new ContribuableIntrouvableException(nif));
    }

    public List<Contribuable> tous() {
        return repositoryPort.findAll();
    }

    public Page<Contribuable> tousPagine(Pageable pageable) {
        return repositoryPort.findAll(pageable);
    }
}
