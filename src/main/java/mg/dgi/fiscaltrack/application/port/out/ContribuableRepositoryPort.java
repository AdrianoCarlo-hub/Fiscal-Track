package mg.dgi.fiscaltrack.application.port.out;

import mg.dgi.fiscaltrack.domain.model.Contribuable;

import java.util.List;
import java.util.Optional;

public interface ContribuableRepositoryPort {

    Contribuable save(Contribuable contribuable);

    Optional<Contribuable> findByNif(String nif);

    List<Contribuable> findAll();

    void deleteByNif(String nif);

    boolean existsByNif(String nif);

    List<Contribuable> search(String query);
}
