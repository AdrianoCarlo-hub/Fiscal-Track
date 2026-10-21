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

    /** Derniere declaration deposee pour un NIF (via ses obligations). */
    Optional<Declaration> findDerniereDeclarationByNif(String nif);

    /** Derniere declaration d'un impot donne pour un NIF (via ses obligations). */
    Optional<Declaration> findDerniereDeclarationByNifEtImpot(String nif, String codeImpot);

    /**
     * Derniere declaration VALIDEE d'un impot dont l'exercice est strictement
     * anterieur a {@code exercice} (base des acomptes obligatoires).
     */
    Optional<Declaration> findDerniereDeclarationValideeAvantExercice(String nif, String codeImpot,
                                                                     int exercice);

    List<Declaration> findAll();

    Page<Declaration> findAll(Pageable pageable);

    List<Declaration> findByStatutValidation(String statutValidation);

    void deleteById(Long id);

    boolean existsByIdObligationFiscale(Long idObligationFiscale);
}
