package mg.dgi.fiscaltrack.application.usecase.admin;

import mg.dgi.fiscaltrack.application.port.out.JourFerieRepositoryPort;
import mg.dgi.fiscaltrack.domain.model.JourFerie;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class GererJoursFeriesUseCase {

    private final JourFerieRepositoryPort jourFerieRepositoryPort;

    public GererJoursFeriesUseCase(JourFerieRepositoryPort jourFerieRepositoryPort) {
        this.jourFerieRepositoryPort = jourFerieRepositoryPort;
    }

    public JourFerie ajouter(LocalDate date, String description) {
        if (date == null) {
            throw new IllegalArgumentException("La date est obligatoire");
        }
        if (jourFerieRepositoryPort.existsByDate(date)) {
            throw new IllegalArgumentException(
                    "Un jour ferie existe deja pour cette date : " + date);
        }
        JourFerie jour = JourFerie.builder()
                .dateFerie(date)
                .description(description)
                .build();
        return jourFerieRepositoryPort.save(jour);
    }

    public void supprimer(Long idJourFerie) {
        jourFerieRepositoryPort.deleteById(idJourFerie);
    }

    public List<JourFerie> parAnnee(int annee) {
        return jourFerieRepositoryPort.findByAnnee(annee);
    }

    public List<JourFerie> tous() {
        return jourFerieRepositoryPort.findAll();
    }
}
