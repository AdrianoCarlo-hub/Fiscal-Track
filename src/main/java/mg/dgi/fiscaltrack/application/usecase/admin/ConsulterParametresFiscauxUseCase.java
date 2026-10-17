package mg.dgi.fiscaltrack.application.usecase.admin;

import mg.dgi.fiscaltrack.application.port.out.ParametreFiscalRepositoryPort;
import mg.dgi.fiscaltrack.domain.model.ParametreFiscal;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Consultation des parametres fiscaux (page ADMIN).
 */
@Service
public class ConsulterParametresFiscauxUseCase {

    private final ParametreFiscalRepositoryPort repositoryPort;

    public ConsulterParametresFiscauxUseCase(ParametreFiscalRepositoryPort repositoryPort) {
        this.repositoryPort = repositoryPort;
    }

    public List<ParametreFiscal> tous() {
        return repositoryPort.findAll();
    }

    public List<ParametreFiscal> parSecteur(String secteur) {
        return repositoryPort.findBySecteur(secteur);
    }

    public List<ParametreFiscal> applicablesPourAnnee(int annee) {
        return repositoryPort.findApplicablesPourAnnee(annee);
    }

    public ParametreFiscal parCode(String code) {
        return repositoryPort.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Parametre fiscal introuvable : " + code));
    }
}
