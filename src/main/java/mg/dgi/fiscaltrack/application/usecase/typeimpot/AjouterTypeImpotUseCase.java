package mg.dgi.fiscaltrack.application.usecase.typeimpot;

import mg.dgi.fiscaltrack.application.port.out.TypeImpotRepositoryPort;
import mg.dgi.fiscaltrack.domain.enums.Periodicite;
import mg.dgi.fiscaltrack.domain.model.TypeImpot;
import org.springframework.stereotype.Service;

@Service
public class AjouterTypeImpotUseCase {

    private final TypeImpotRepositoryPort repositoryPort;

    public AjouterTypeImpotUseCase(TypeImpotRepositoryPort repositoryPort) {
        this.repositoryPort = repositoryPort;
    }

    public TypeImpot execute(TypeImpot typeImpot) {
        if (typeImpot.getCodeImpot() == null || typeImpot.getCodeImpot().isBlank()) {
            throw new IllegalArgumentException("Le code impot est obligatoire");
        }
        if (typeImpot.getNomImpot() == null || typeImpot.getNomImpot().isBlank()) {
            throw new IllegalArgumentException("Le nom de l'impot est obligatoire");
        }
        if (typeImpot.getPeriodicite() == null) {
            throw new IllegalArgumentException("La periodicite est obligatoire");
        }
        if (repositoryPort.existsByCodeImpot(typeImpot.getCodeImpot())) {
            throw new IllegalArgumentException("Un type d'impot avec ce code existe deja");
        }

        validerEcheance(typeImpot);

        return repositoryPort.save(typeImpot);
    }

    private void validerEcheance(TypeImpot typeImpot) {
        Integer jour = typeImpot.getEcheanceTheoriqueJour();
        Integer mois = typeImpot.getEcheanceTheoriqueMois();

        if (jour == null || jour < 1 || jour > 31) {
            throw new IllegalArgumentException("Le jour d'echeance doit etre entre 1 et 31");
        }
        if (mois != null && (mois < 1 || mois > 12)) {
            throw new IllegalArgumentException("Le mois d'echeance doit etre entre 1 et 12");
        }
        if (typeImpot.getPeriodicite() == Periodicite.ANNUELLE && mois == null) {
            throw new IllegalArgumentException("Un impot annuel doit avoir un mois d'echeance");
        }
    }
}
