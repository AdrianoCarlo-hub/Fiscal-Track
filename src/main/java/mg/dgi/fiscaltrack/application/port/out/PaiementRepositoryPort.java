package mg.dgi.fiscaltrack.application.port.out;

import mg.dgi.fiscaltrack.domain.model.Paiement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface PaiementRepositoryPort {

    Paiement save(Paiement paiement);

    Optional<Paiement> findById(Long id);

    List<Paiement> findAll();

    Page<Paiement> findAll(Pageable pageable);

    List<Paiement> findByIdCompte(Long idCompte);

    Optional<Paiement> findByReferenceTransaction(String referenceTransaction);

    void deleteById(Long id);

    boolean existsByReferenceTransaction(String referenceTransaction);
}
