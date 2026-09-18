package mg.dgi.fiscaltrack.infrastructure.persistence.repository;

import mg.dgi.fiscaltrack.infrastructure.persistence.entity.DeclarationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeclarationJpaRepository extends JpaRepository<DeclarationEntity, Long> {

    Optional<DeclarationEntity> findByIdObligationFiscale(Long idObligationFiscale);

    boolean existsByIdObligationFiscale(Long idObligationFiscale);

    List<DeclarationEntity> findByStatutValidation(String statutValidation);
}
