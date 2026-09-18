package mg.dgi.fiscaltrack.application.port.out;

import mg.dgi.fiscaltrack.domain.model.JourFerie;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface JourFerieRepositoryPort {

    JourFerie save(JourFerie jourFerie);

    Optional<JourFerie> findById(Long id);

    Optional<JourFerie> findByDate(LocalDate date);

    List<JourFerie> findAll();

    List<JourFerie> findByAnnee(int annee);

    void deleteById(Long id);

    boolean existsByDate(LocalDate date);
}
