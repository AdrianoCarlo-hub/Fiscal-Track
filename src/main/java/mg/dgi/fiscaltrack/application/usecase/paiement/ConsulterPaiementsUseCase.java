package mg.dgi.fiscaltrack.application.usecase.paiement;

import mg.dgi.fiscaltrack.application.port.out.PaiementRepositoryPort;
import mg.dgi.fiscaltrack.domain.model.Paiement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ConsulterPaiementsUseCase {

    private final PaiementRepositoryPort repositoryPort;

    public ConsulterPaiementsUseCase(PaiementRepositoryPort repositoryPort) {
        this.repositoryPort = repositoryPort;
    }

    public Paiement parId(Long id) {
        return repositoryPort.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Paiement introuvable : " + id));
    }

    public List<Paiement> parCompte(Long idCompte) {
        return repositoryPort.findByIdCompte(idCompte);
    }

    public List<Paiement> tous() {
        return repositoryPort.findAll();
    }

    public Page<Paiement> tousPaginees(Pageable pageable) {
        return repositoryPort.findAll(pageable);
    }
}
