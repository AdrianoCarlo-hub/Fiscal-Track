package mg.dgi.fiscaltrack.application.usecase.declaration;

import mg.dgi.fiscaltrack.application.port.out.DeclarationRepositoryPort;
import mg.dgi.fiscaltrack.domain.enums.StatutValidation;
import mg.dgi.fiscaltrack.domain.model.Declaration;
import org.springframework.stereotype.Service;

@Service
public class VerifierDeclarationUseCase {

    private final DeclarationRepositoryPort repositoryPort;

    public VerifierDeclarationUseCase(DeclarationRepositoryPort repositoryPort) {
        this.repositoryPort = repositoryPort;
    }

    public Declaration valider(Long idDeclaration) {
        return changerStatut(idDeclaration, StatutValidation.VALIDEE);
    }

    public Declaration rejeter(Long idDeclaration) {
        return changerStatut(idDeclaration, StatutValidation.REJETEE);
    }

    private Declaration changerStatut(Long idDeclaration, StatutValidation nouveauStatut) {
        Declaration declaration = repositoryPort.findById(idDeclaration)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Declaration introuvable : " + idDeclaration));

        if (declaration.getStatutValidation() != StatutValidation.EN_ATTENTE_VALIDATION) {
            throw new IllegalArgumentException(
                    "Seule une declaration en attente peut etre modifiee");
        }

        declaration.setStatutValidation(nouveauStatut);
        return repositoryPort.save(declaration);
    }
}
