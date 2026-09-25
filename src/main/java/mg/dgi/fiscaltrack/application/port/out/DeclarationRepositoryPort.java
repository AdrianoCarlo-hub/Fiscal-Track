package mg.dgi.fiscaltrack.application.port.out;

import mg.dgi.fiscaltrack.domain.model.Declaration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface DeclarationRepositoryPort {

    Declaration save(Declaration declaration);

    Optional<Declaration> findById(Long id);

    Optional<Declaration> findByIdObligationFiscale(Long idObligationFiscale);

    List<Declaration> findAll();

    Page<Declaration> findAll(Pageable pageable);

    List<Declaration> findByStatutValidation(String statutValidation);

    void deleteById(Long id);

    boolean existsByIdObligationFiscale(Long idObligationFiscale);
}
