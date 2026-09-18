package mg.dgi.fiscaltrack.application.usecase.auth;

import mg.dgi.fiscaltrack.application.port.out.ContribuableRepositoryPort;
import mg.dgi.fiscaltrack.domain.enums.StatutActivite;
import mg.dgi.fiscaltrack.domain.exception.AccesNonAutoriseException;
import mg.dgi.fiscaltrack.domain.exception.ContribuableIntrouvableException;
import mg.dgi.fiscaltrack.domain.model.Contribuable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthentifierContribuableUseCase {

    private final ContribuableRepositoryPort contribuableRepositoryPort;
    private final PasswordEncoder passwordEncoder;

    public AuthentifierContribuableUseCase(ContribuableRepositoryPort contribuableRepositoryPort,
                                            PasswordEncoder passwordEncoder) {
        this.contribuableRepositoryPort = contribuableRepositoryPort;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Verifie le NIF et le mot de passe. Le mot de passe est compare au
     * hash bcrypt stocke en base. Si le compte est RADIE, l'acces est refuse.
     */
    public Contribuable execute(String nif, String motDePasse) {
        if (nif == null || nif.isBlank()) {
            throw new AccesNonAutoriseException("Le NIF est obligatoire");
        }
        if (motDePasse == null || motDePasse.isBlank()) {
            throw new AccesNonAutoriseException("Le mot de passe est obligatoire");
        }

        Contribuable contribuable = contribuableRepositoryPort.findByNif(nif)
                .orElseThrow(() -> new ContribuableIntrouvableException(nif));

        if (!passwordEncoder.matches(motDePasse, contribuable.getMotDePasseHashContribuable())) {
            throw new AccesNonAutoriseException("Mot de passe incorrect");
        }

        if (contribuable.getStatutActivite() == StatutActivite.RADIE) {
            throw new AccesNonAutoriseException(
                    "Ce compte a ete radie, connexion impossible");
        }

        return contribuable;
    }
}
