package mg.dgi.fiscaltrack.application.usecase.contribuable;

import mg.dgi.fiscaltrack.application.port.out.ContribuableRepositoryPort;
import mg.dgi.fiscaltrack.application.usecase.historique.EnregistrerActionUseCase;
import mg.dgi.fiscaltrack.domain.enums.CanalEnvoi;
import mg.dgi.fiscaltrack.domain.exception.ContribuableIntrouvableException;
import mg.dgi.fiscaltrack.domain.model.Contribuable;
import org.springframework.stereotype.Service;

/**
 * Mise a jour des preferences de contact d'un contribuable : canal prefere
 * (null = tous) et consentements e-mail / SMS.
 */
@Service
public class ModifierPreferencesContactUseCase {

    private final ContribuableRepositoryPort repositoryPort;
    private final EnregistrerActionUseCase enregistrerActionUseCase;

    public ModifierPreferencesContactUseCase(ContribuableRepositoryPort repositoryPort,
                                             EnregistrerActionUseCase enregistrerActionUseCase) {
        this.repositoryPort = repositoryPort;
        this.enregistrerActionUseCase = enregistrerActionUseCase;
    }

    /**
     * @param canalPrefere null = inchangé, chaîne vide = tous les canaux
     */
    public Contribuable execute(String nif, String canalPrefere,
                                Boolean accepteEmail, Boolean accepteSms, String auteur) {
        Contribuable existant = repositoryPort.findByNif(nif)
                .orElseThrow(() -> new ContribuableIntrouvableException(nif));

        if (canalPrefere != null) {
            existant.setCanalPrefere(canalPrefere.isBlank()
                    ? null
                    : CanalEnvoi.valueOf(canalPrefere.trim()));
        }
        if (accepteEmail != null) {
            existant.setAccepteEmail(accepteEmail);
        }
        if (accepteSms != null) {
            existant.setAccepteSms(accepteSms);
        }

        Contribuable saved = repositoryPort.save(existant);
        enregistrerActionUseCase.execute(
                auteur == null ? nif : auteur,
                "MODIFICATION_PREFERENCES_CONTACT",
                "Mise a jour des preferences de contact du contribuable " + nif);
        return saved;
    }
}
