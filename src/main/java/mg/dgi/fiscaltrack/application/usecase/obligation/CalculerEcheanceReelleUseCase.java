package mg.dgi.fiscaltrack.application.usecase.obligation;

import mg.dgi.fiscaltrack.application.port.out.JourFerieRepositoryPort;
import mg.dgi.fiscaltrack.domain.model.JourFerie;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;

@Service
public class CalculerEcheanceReelleUseCase {

    /**
     * Resultat du report d'une echeance.
     *
     * @param date        la date reelle (premier jour ouvrable)
     * @param idJourFerie identifiant du dernier jour ferie ayant provoque le
     *                    decalage (null si seul un week-end a decale la date)
     */
    public record EcheanceReelle(LocalDate date, Long idJourFerie) {
    }

    private final JourFerieRepositoryPort jourFerieRepositoryPort;

    public CalculerEcheanceReelleUseCase(JourFerieRepositoryPort jourFerieRepositoryPort) {
        this.jourFerieRepositoryPort = jourFerieRepositoryPort;
    }

    public LocalDate execute(LocalDate dateTheorique) {
        return calculer(dateTheorique).date();
    }

    /**
     * Calcule la date reelle en reportant au premier jour ouvrable suivant
     * (CPF VII-32) et rapporte le jour ferie eventuellement responsable du
     * decalage.
     */
    public EcheanceReelle calculer(LocalDate dateTheorique) {
        if (dateTheorique == null) {
            throw new IllegalArgumentException("La date theorique est obligatoire");
        }

        LocalDate date = dateTheorique;
        Long idJourFerie = null;
        while (estJourNonOuvrable(date)) {
            if (jourFerieRepositoryPort.existsByDate(date)) {
                idJourFerie = jourFerieRepositoryPort.findByDate(date)
                        .map(JourFerie::getIdJourFerie)
                        .orElse(null);
            }
            date = date.plusDays(1);
        }
        return new EcheanceReelle(date, idJourFerie);
    }

    private boolean estJourNonOuvrable(LocalDate date) {
        DayOfWeek jour = date.getDayOfWeek();
        if (jour == DayOfWeek.SATURDAY || jour == DayOfWeek.SUNDAY) {
            return true;
        }
        return jourFerieRepositoryPort.existsByDate(date);
    }
}
