package mg.dgi.fiscaltrack.application.usecase.contribuable;

import mg.dgi.fiscaltrack.application.port.out.ContribuableRepositoryPort;
import mg.dgi.fiscaltrack.domain.model.Contribuable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RechercherContribuableUseCase {

    private final ContribuableRepositoryPort repositoryPort;

    public RechercherContribuableUseCase(ContribuableRepositoryPort repositoryPort) {
        this.repositoryPort = repositoryPort;
    }

    public List<Contribuable> execute(String critere) {
        if (critere == null || critere.isBlank()) {
            return repositoryPort.findAll();
        }
        return repositoryPort.search(critere.trim());
    }

    public Page<Contribuable> executePagine(String critere, Pageable pageable) {
        if (critere == null || critere.isBlank()) {
            return repositoryPort.findAll(pageable);
        }
        return repositoryPort.search(critere.trim(), pageable);
    }
}
