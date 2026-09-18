package mg.dgi.fiscaltrack.application.usecase.contribuable;

import mg.dgi.fiscaltrack.application.port.out.ContribuableRepositoryPort;
import mg.dgi.fiscaltrack.domain.model.Contribuable;
import org.springframework.stereotype.Service;

@Service
public class AjouterContribuableUseCase {

    private final ContribuableRepositoryPort repositoryPort;

    public AjouterContribuableUseCase(ContribuableRepositoryPort repositoryPort) {
        this.repositoryPort = repositoryPort;
    }

    public Contribuable execute(Contribuable contribuable) {
        if (contribuable.getNif() == null || contribuable.getNif().length() != 10) {
            throw new IllegalArgumentException("Le NIF doit contenir exactement 10 caracteres");
        }
        if (repositoryPort.existsByNif(contribuable.getNif())) {
            throw new IllegalArgumentException("Un contribuable avec ce NIF existe deja");
        }
        if (contribuable.getStatutActivite() == null) {
            contribuable.setStatutActivite(
                    mg.dgi.fiscaltrack.domain.enums.StatutActivite.ACTIF);
        }
        return repositoryPort.save(contribuable);
    }
}
