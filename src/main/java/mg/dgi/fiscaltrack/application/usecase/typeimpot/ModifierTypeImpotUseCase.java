package mg.dgi.fiscaltrack.application.usecase.typeimpot;

import mg.dgi.fiscaltrack.application.port.out.TypeImpotRepositoryPort;
import mg.dgi.fiscaltrack.domain.model.TypeImpot;
import org.springframework.stereotype.Service;

@Service
public class ModifierTypeImpotUseCase {

    private final TypeImpotRepositoryPort repositoryPort;

    public ModifierTypeImpotUseCase(TypeImpotRepositoryPort repositoryPort) {
        this.repositoryPort = repositoryPort;
    }

    public TypeImpot execute(String codeImpot, TypeImpot donneesModifiees) {
        TypeImpot existant = repositoryPort.findByCodeImpot(codeImpot)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Type d'impot introuvable avec le code : " + codeImpot));

        if (donneesModifiees.getNomImpot() != null) {
            existant.setNomImpot(donneesModifiees.getNomImpot());
        }
        if (donneesModifiees.getPeriodicite() != null) {
            existant.setPeriodicite(donneesModifiees.getPeriodicite());
        }
        if (donneesModifiees.getEcheanceTheoriqueJour() != null) {
            existant.setEcheanceTheoriqueJour(donneesModifiees.getEcheanceTheoriqueJour());
        }
        if (donneesModifiees.getEcheanceTheoriqueMois() != null) {
            existant.setEcheanceTheoriqueMois(donneesModifiees.getEcheanceTheoriqueMois());
        }

        return repositoryPort.save(existant);
    }
}
