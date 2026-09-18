package mg.dgi.fiscaltrack.application.usecase.admin;

import mg.dgi.fiscaltrack.application.port.out.TypeImpotRepositoryPort;
import mg.dgi.fiscaltrack.domain.model.TypeImpot;
import org.springframework.stereotype.Service;

@Service
public class ParametrerReglesFiscalesUseCase {

    private final TypeImpotRepositoryPort typeImpotRepositoryPort;

    public ParametrerReglesFiscalesUseCase(TypeImpotRepositoryPort typeImpotRepositoryPort) {
        this.typeImpotRepositoryPort = typeImpotRepositoryPort;
    }

    public TypeImpot definirEcheanceTheorique(String codeImpot,
                                                Integer jourEcheance,
                                                Integer moisEcheance) {
        TypeImpot typeImpot = typeImpotRepositoryPort.findByCodeImpot(codeImpot)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Type d'impot introuvable : " + codeImpot));

        if (jourEcheance == null || jourEcheance < 1 || jourEcheance > 31) {
            throw new IllegalArgumentException("Le jour d'echeance doit etre entre 1 et 31");
        }
        if (moisEcheance != null && (moisEcheance < 1 || moisEcheance > 12)) {
            throw new IllegalArgumentException("Le mois d'echeance doit etre entre 1 et 12");
        }

        typeImpot.setEcheanceTheoriqueJour(jourEcheance);
        typeImpot.setEcheanceTheoriqueMois(moisEcheance);
        return typeImpotRepositoryPort.save(typeImpot);
    }
}
