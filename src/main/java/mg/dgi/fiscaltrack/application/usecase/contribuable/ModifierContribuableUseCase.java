package mg.dgi.fiscaltrack.application.usecase.contribuable;

import mg.dgi.fiscaltrack.application.port.out.ContribuableRepositoryPort;
import mg.dgi.fiscaltrack.application.usecase.historique.EnregistrerActionUseCase;
import mg.dgi.fiscaltrack.domain.exception.ContribuableIntrouvableException;
import mg.dgi.fiscaltrack.domain.model.Contribuable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class ModifierContribuableUseCase {

    private final ContribuableRepositoryPort repositoryPort;
    private final PasswordEncoder passwordEncoder;
    private final EnregistrerActionUseCase enregistrerActionUseCase;

    public ModifierContribuableUseCase(ContribuableRepositoryPort repositoryPort,
                                       PasswordEncoder passwordEncoder,
                                       EnregistrerActionUseCase enregistrerActionUseCase) {
        this.repositoryPort = repositoryPort;
        this.passwordEncoder = passwordEncoder;
        this.enregistrerActionUseCase = enregistrerActionUseCase;
    }

    public Contribuable execute(String nif, Contribuable donneesModifiees) {
        return execute(nif, donneesModifiees, null);
    }

    /**
     * Modifie un contribuable en appliquant un patch strict : seuls les
     * champs non-null de donneesModifiees sont ecrits. Les champs null
     * ne modifient pas la valeur existante. Aligne sur le comportement
     * de ModifierTypeImpotUseCase.
     *
     * Le mot de passe n'est re-encode que si un nouveau mot de passe est
     * fourni (champ non-null et non-blanc) ; sinon l'ancien hash est conserve.
     */
    public Contribuable execute(String nif, Contribuable donneesModifiees, String auteur) {
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
        if (donneesModifiees.getMotDePasseHashContribuable() != null
                && !donneesModifiees.getMotDePasseHashContribuable().isBlank()) {
            existant.setMotDePasseHashContribuable(
                    passwordEncoder.encode(donneesModifiees.getMotDePasseHashContribuable()));
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
        if (donneesModifiees.getDateCloture() != null) {
            existant.setDateCloture(donneesModifiees.getDateCloture());
        }

        Contribuable saved = repositoryPort.save(existant);

        enregistrerActionUseCase.execute(
                auteur == null ? saved.getNif() : auteur,
                "MODIFICATION_CONTRIBUABLE",
                "Modification du contribuable " + saved.getNif());
        return saved;
    }
}
