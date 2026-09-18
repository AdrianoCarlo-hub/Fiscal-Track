package mg.dgi.fiscaltrack.application.usecase.contribuable;

import mg.dgi.fiscaltrack.application.port.out.ContribuableRepositoryPort;
import mg.dgi.fiscaltrack.domain.exception.ContribuableIntrouvableException;
import mg.dgi.fiscaltrack.domain.model.Contribuable;
import org.springframework.stereotype.Service;

@Service
public class ModifierContribuableUseCase {

    private final ContribuableRepositoryPort repositoryPort;

    public ModifierContribuableUseCase(ContribuableRepositoryPort repositoryPort) {
        this.repositoryPort = repositoryPort;
    }

    public Contribuable execute(String nif, Contribuable donneesModifiees) {
        Contribuable existant = repositoryPort.findByNif(nif)
                .orElseThrow(() -> new ContribuableIntrouvableException(nif));

        existant.setRaisonSociale(donneesModifiees.getRaisonSociale());
        existant.setFormeJuridique(donneesModifiees.getFormeJuridique());
        existant.setNomDirigeant(donneesModifiees.getNomDirigeant());
        existant.setPrenomDirigeant(donneesModifiees.getPrenomDirigeant());
        existant.setCinDirigeant(donneesModifiees.getCinDirigeant());
        existant.setEmailContribuable(donneesModifiees.getEmailContribuable());
        existant.setTelephoneContribuable(donneesModifiees.getTelephoneContribuable());
        existant.setAdresseContribuable(donneesModifiees.getAdresseContribuable());
        existant.setCommuneContribuable(donneesModifiees.getCommuneContribuable());
        existant.setRegimeImposition(donneesModifiees.getRegimeImposition());
        existant.setObligationComptable(donneesModifiees.getObligationComptable());
        existant.setStatutActivite(donneesModifiees.getStatutActivite());

        return repositoryPort.save(existant);
    }
}
