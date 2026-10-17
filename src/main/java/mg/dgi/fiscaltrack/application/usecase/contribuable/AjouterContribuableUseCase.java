package mg.dgi.fiscaltrack.application.usecase.contribuable;

import mg.dgi.fiscaltrack.application.port.out.ContribuableRepositoryPort;
import mg.dgi.fiscaltrack.application.usecase.historique.EnregistrerActionUseCase;
import mg.dgi.fiscaltrack.domain.model.Contribuable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AjouterContribuableUseCase {

    private final ContribuableRepositoryPort repositoryPort;
    private final PasswordEncoder passwordEncoder;
    private final EnregistrerActionUseCase enregistrerActionUseCase;

    public AjouterContribuableUseCase(ContribuableRepositoryPort repositoryPort,
                                      PasswordEncoder passwordEncoder,
                                      EnregistrerActionUseCase enregistrerActionUseCase) {
        this.repositoryPort = repositoryPort;
        this.passwordEncoder = passwordEncoder;
        this.enregistrerActionUseCase = enregistrerActionUseCase;
    }

    public Contribuable execute(Contribuable contribuable) {
        return execute(contribuable, null);
    }

    /**
     * Cree un contribuable. Le mot de passe fourni est encode (BCrypt) avant
     * persistance ; seul le hash est stocke et il n'est jamais renvoye par l'API.
     *
     * @param auteur identifiant de l'agent a l'origine de l'action (audit),
     *               ou null si inconnu.
     */
    public Contribuable execute(Contribuable contribuable, String auteur) {
        if (contribuable.getNif() == null || contribuable.getNif().length() != 10) {
            throw new IllegalArgumentException("Le NIF doit contenir exactement 10 caracteres");
        }
        if (repositoryPort.existsByNif(contribuable.getNif())) {
            throw new IllegalArgumentException("Un contribuable avec ce NIF existe deja");
        }
        if (contribuable.getMotDePasseHashContribuable() == null
                || contribuable.getMotDePasseHashContribuable().isBlank()) {
            throw new IllegalArgumentException("Le mot de passe est obligatoire");
        }
        if (contribuable.getStatutActivite() == null) {
            contribuable.setStatutActivite(
                    mg.dgi.fiscaltrack.domain.enums.StatutActivite.ACTIF);
        }
        contribuable.setMotDePasseHashContribuable(
                passwordEncoder.encode(contribuable.getMotDePasseHashContribuable()));
        Contribuable saved = repositoryPort.save(contribuable);

        enregistrerActionUseCase.execute(
                auteur == null ? saved.getNif() : auteur,
                "CREATION_CONTRIBUABLE",
                "Creation du contribuable " + saved.getNif()
                        + " (" + saved.getRaisonSociale() + ")");
        return saved;
    }
}
