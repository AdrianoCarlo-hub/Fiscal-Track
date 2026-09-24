package mg.dgi.fiscaltrack.application.usecase.declaration;

import mg.dgi.fiscaltrack.application.port.out.DeclarationRepositoryPort;
import mg.dgi.fiscaltrack.application.usecase.historique.EnregistrerActionUseCase;
import mg.dgi.fiscaltrack.domain.enums.StatutValidation;
import mg.dgi.fiscaltrack.domain.model.Declaration;
import org.springframework.stereotype.Service;

@Service
public class VerifierDeclarationUseCase {

    private final DeclarationRepositoryPort repositoryPort;
    private final EnregistrerActionUseCase enregistrerActionUseCase;

    public VerifierDeclarationUseCase(DeclarationRepositoryPort repositoryPort,
                                        EnregistrerActionUseCase enregistrerActionUseCase) {
        this.repositoryPort = repositoryPort;
        this.enregistrerActionUseCase = enregistrerActionUseCase;
    }

    public Declaration valider(Long idDeclaration) {
        Declaration d = changerStatut(idDeclaration, StatutValidation.VALIDEE);
        enregistrerActionUseCase.execute("AGENT_GESTION", "VALIDATION_DECLARATION",
                "Declaration #" + idDeclaration + " validee");
        return d;
    }

    public Declaration rejeter(Long idDeclaration) {
        Declaration d = changerStatut(idDeclaration, StatutValidation.REJETEE);
        enregistrerActionUseCase.execute("AGENT_GESTION", "REJET_DECLARATION",
                "Declaration #" + idDeclaration + " rejetee");
        return d;
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
