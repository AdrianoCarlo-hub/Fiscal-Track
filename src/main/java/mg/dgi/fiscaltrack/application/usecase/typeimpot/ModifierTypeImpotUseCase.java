package mg.dgi.fiscaltrack.application.usecase.typeimpot;

import mg.dgi.fiscaltrack.application.port.out.TypeImpotRepositoryPort;
import mg.dgi.fiscaltrack.application.usecase.historique.EnregistrerActionUseCase;
import mg.dgi.fiscaltrack.domain.model.TypeImpot;
import org.springframework.stereotype.Service;

@Service
public class ModifierTypeImpotUseCase {

    private final TypeImpotRepositoryPort repositoryPort;
    private final EnregistrerActionUseCase enregistrerActionUseCase;

    public ModifierTypeImpotUseCase(TypeImpotRepositoryPort repositoryPort,
                                    EnregistrerActionUseCase enregistrerActionUseCase) {
        this.repositoryPort = repositoryPort;
        this.enregistrerActionUseCase = enregistrerActionUseCase;
    }

    public TypeImpot execute(String codeImpot, TypeImpot donneesModifiees) {
        return execute(codeImpot, donneesModifiees, null);
    }

    public TypeImpot execute(String codeImpot, TypeImpot donneesModifiees, String auteur) {
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

        TypeImpot saved = repositoryPort.save(existant);

        enregistrerActionUseCase.execute(
                auteur == null ? "SYSTEME" : auteur,
                "MODIFICATION_TYPE_IMPOT",
                "Modification du type d'impot " + saved.getCodeImpot());
        return saved;
    }
}
