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

    /**
     * Modifie un contribuable en appliquant un patch strict : seuls les
     * champs non-null de donneesModifiees sont ecrits. Les champs null
     * ne modifient pas la valeur existante. Aligne sur le comportement
     * de ModifierTypeImpotUseCase.
     */
    public Contribuable execute(String nif, Contribuable donneesModifiees) {
        Contribuable existant = repositoryPort.findByNif(nif)
                .orElseThrow(() -> new ContribuableIntrouvableException(nif));

        if (donneesModifiees.getRaisonSociale() != null) {
            existant.setRaisonSociale(donneesModifiees.getRaisonSociale());
        }
        if (donneesModifiees.getFormeJuridique() != null) {
            existant.setFormeJuridique(donneesModifiees.getFormeJuridique());
        }
        if (donneesModifiees.getNomDirigeant() != null) {
            existant.setNomDirigeant(donneesModifiees.getNomDirigeant());
        }
        if (donneesModifiees.getPrenomDirigeant() != null) {
            existant.setPrenomDirigeant(donneesModifiees.getPrenomDirigeant());
        }
        if (donneesModifiees.getCinDirigeant() != null) {
            existant.setCinDirigeant(donneesModifiees.getCinDirigeant());
        }
        if (donneesModifiees.getEmailContribuable() != null) {
            existant.setEmailContribuable(donneesModifiees.getEmailContribuable());
        }
        if (donneesModifiees.getTelephoneContribuable() != null) {
            existant.setTelephoneContribuable(donneesModifiees.getTelephoneContribuable());
        }
        if (donneesModifiees.getAdresseContribuable() != null) {
            existant.setAdresseContribuable(donneesModifiees.getAdresseContribuable());
        }
        if (donneesModifiees.getCommuneContribuable() != null) {
            existant.setCommuneContribuable(donneesModifiees.getCommuneContribuable());
        }
        if (donneesModifiees.getRegimeImposition() != null) {
            existant.setRegimeImposition(donneesModifiees.getRegimeImposition());
        }
        if (donneesModifiees.getObligationComptable() != null) {
            existant.setObligationComptable(donneesModifiees.getObligationComptable());
        }
        if (donneesModifiees.getStatutActivite() != null) {
            existant.setStatutActivite(donneesModifiees.getStatutActivite());
        }

        return repositoryPort.save(existant);
    }
}
