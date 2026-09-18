package mg.dgi.fiscaltrack.application.port.out;

import mg.dgi.fiscaltrack.domain.model.Declaration;

import java.util.List;
import java.util.Optional;

public interface DeclarationRepositoryPort {

    Declaration save(Declaration declaration);

    Optional<Declaration> findById(Long id);

    Optional<Declaration> findByIdObligationFiscale(Long idObligationFiscale);

    List<Declaration> findAll();

    List<Declaration> findByStatutValidation(String statutValidation);

    void deleteById(Long id);

    boolean existsByIdObligationFiscale(Long idObligationFiscale);
}
