package mg.dgi.fiscaltrack.application.usecase.calendrier;

import mg.dgi.fiscaltrack.application.port.out.ObligationFiscaleRepositoryPort;
import mg.dgi.fiscaltrack.domain.enums.StatutDeclaration;
import mg.dgi.fiscaltrack.domain.model.ObligationFiscale;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
public class ConsulterCalendrierUseCase {

    private final ObligationFiscaleRepositoryPort obligationRepositoryPort;

    public ConsulterCalendrierUseCase(ObligationFiscaleRepositoryPort obligationRepositoryPort) {
        this.obligationRepositoryPort = obligationRepositoryPort;
    }

    /**
     * Retourne les obligations d'un contribuable triees par date limite,
     * filtrees sur une annee donnee. Utilise pour l'agenda du front-office.
     */
    public List<ObligationFiscale> parContribuableEtAnnee(String nif, int annee) {
        return obligationRepositoryPort.findByNif(nif).stream()
                .filter(o -> o.getDateLimiteReelle() != null
                        && o.getDateLimiteReelle().getYear() == annee)
                .sorted(Comparator.comparing(ObligationFiscale::getDateLimiteReelle))
                .toList();
    }

    /**
     * Retourne les obligations dont l'echeance tombe dans les prochains jours,
     * pour l'affichage des alertes du tableau de bord.
     */
    public List<ObligationFiscale> echeancesProchaines(String nif, int nombreJours) {
        LocalDate aujourdHui = LocalDate.now();
        LocalDate limite = aujourdHui.plusDays(nombreJours);

        return obligationRepositoryPort.findByNif(nif).stream()
                .filter(o -> o.getDateLimiteReelle() != null
                        && !o.getDateLimiteReelle().isBefore(aujourdHui)
                        && !o.getDateLimiteReelle().isAfter(limite)
                        && o.getStatutDeclaration() == StatutDeclaration.ATTENTE)
                .sorted(Comparator.comparing(ObligationFiscale::getDateLimiteReelle))
                .toList();
    }
}
