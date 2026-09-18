package mg.dgi.fiscaltrack.application.usecase.obligation;

import mg.dgi.fiscaltrack.application.port.out.ObligationFiscaleRepositoryPort;
import mg.dgi.fiscaltrack.domain.enums.StatutDeclaration;
import mg.dgi.fiscaltrack.domain.model.ObligationFiscale;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class IdentifierRetardsUseCase {

    private final ObligationFiscaleRepositoryPort repositoryPort;

    public IdentifierRetardsUseCase(ObligationFiscaleRepositoryPort repositoryPort) {
        this.repositoryPort = repositoryPort;
    }

    /**
     * Parcourt les obligations en retard, passe leur statut a RETARD
     * et retourne la liste mise a jour.
     */
    public List<ObligationFiscale> execute() {
        LocalDate aujourdHui = LocalDate.now();
        List<ObligationFiscale> enRetard = repositoryPort.findEnRetard(aujourdHui);

        for (ObligationFiscale obligation : enRetard) {
            if (obligation.getStatutDeclaration() != StatutDeclaration.RETARD) {
                obligation.setStatutDeclaration(StatutDeclaration.RETARD);
                repositoryPort.save(obligation);
            }
        }
        return enRetard;
    }

    /**
     * Retourne les obligations dont l'echeance tombe dans les prochains jours,
     * utilise pour les rappels J-7 et J-1.
     */
    public List<ObligationFiscale> echeancesProches(int nombreJours) {
        return repositoryPort.findEcheancesProches(LocalDate.now(), nombreJours);
    }
}
