package mg.dgi.fiscaltrack.application.usecase.declaration;

import mg.dgi.fiscaltrack.application.port.out.DeclarationRepositoryPort;
import mg.dgi.fiscaltrack.domain.model.Declaration;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ConsulterDeclarationsUseCase {

    private final DeclarationRepositoryPort repositoryPort;

    public ConsulterDeclarationsUseCase(DeclarationRepositoryPort repositoryPort) {
        this.repositoryPort = repositoryPort;
    }

    public Declaration parId(Long id) {
        return repositoryPort.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Declaration introuvable : " + id));
    }

    public Declaration parObligation(Long idObligation) {
        return repositoryPort.findByIdObligationFiscale(idObligation)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Aucune declaration pour l'obligation : " + idObligation));
    }

    public List<Declaration> toutes() {
        return repositoryPort.findAll();
    }

    public List<Declaration> parStatutValidation(String statut) {
        return repositoryPort.findByStatutValidation(statut);
    }
}
