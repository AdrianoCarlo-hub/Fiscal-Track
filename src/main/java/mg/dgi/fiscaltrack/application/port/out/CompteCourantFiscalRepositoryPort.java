package mg.dgi.fiscaltrack.application.port.out;

import mg.dgi.fiscaltrack.domain.model.CompteCourantFiscal;

import java.util.List;
import java.util.Optional;

public interface CompteCourantFiscalRepositoryPort {

    CompteCourantFiscal save(CompteCourantFiscal compteCourantFiscal);

    Optional<CompteCourantFiscal> findById(Long id);

    Optional<CompteCourantFiscal> findByIdDeclaration(Long idDeclaration);

    List<CompteCourantFiscal> findAll();

    List<CompteCourantFiscal> findByNif(String nif);

    List<CompteCourantFiscal> findByStatut(String statutRecouvrement);

    List<CompteCourantFiscal> findNonSoldes();

    void deleteById(Long id);
}
