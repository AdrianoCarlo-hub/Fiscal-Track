package mg.dgi.fiscaltrack.infrastructure.persistence.repository;

import mg.dgi.fiscaltrack.infrastructure.persistence.entity.PaiementEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaiementJpaRepository extends JpaRepository<PaiementEntity, Long> {

    List<PaiementEntity> findByIdCompte(Long idCompte);

    Optional<PaiementEntity> findByReferenceTransaction(String referenceTransaction);

    boolean existsByReferenceTransaction(String referenceTransaction);
}
