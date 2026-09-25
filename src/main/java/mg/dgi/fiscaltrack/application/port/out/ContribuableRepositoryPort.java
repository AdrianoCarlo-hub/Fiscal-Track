package mg.dgi.fiscaltrack.application.port.out;

import mg.dgi.fiscaltrack.domain.model.Contribuable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface ContribuableRepositoryPort {

    Contribuable save(Contribuable contribuable);

    Optional<Contribuable> findByNif(String nif);

    List<Contribuable> findAll();

    Page<Contribuable> findAll(Pageable pageable);

    void deleteByNif(String nif);

    boolean existsByNif(String nif);

    List<Contribuable> search(String query);

    Page<Contribuable> search(String query, Pageable pageable);
}
