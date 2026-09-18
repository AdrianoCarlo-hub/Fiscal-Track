package mg.dgi.fiscaltrack.application.usecase.obligation;

import mg.dgi.fiscaltrack.application.port.out.JourFerieRepositoryPort;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;

@Service
public class CalculerEcheanceReelleUseCase {

    private final JourFerieRepositoryPort jourFerieRepositoryPort;

    public CalculerEcheanceReelleUseCase(JourFerieRepositoryPort jourFerieRepositoryPort) {
        this.jourFerieRepositoryPort = jourFerieRepositoryPort;
    }

    public LocalDate execute(LocalDate dateTheorique) {
        if (dateTheorique == null) {
            throw new IllegalArgumentException("La date theorique est obligatoire");
        }

        LocalDate date = dateTheorique;
        while (estJourNonOuvrable(date)) {
            date = date.plusDays(1);
        }
        return date;
    }

    private boolean estJourNonOuvrable(LocalDate date) {
        DayOfWeek jour = date.getDayOfWeek();
        if (jour == DayOfWeek.SATURDAY || jour == DayOfWeek.SUNDAY) {
            return true;
        }
        return jourFerieRepositoryPort.existsByDate(date);
    }
}
