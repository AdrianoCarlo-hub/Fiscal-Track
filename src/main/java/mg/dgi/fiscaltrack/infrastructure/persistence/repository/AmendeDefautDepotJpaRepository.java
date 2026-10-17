package mg.dgi.fiscaltrack.infrastructure.persistence.repository;

import mg.dgi.fiscaltrack.infrastructure.persistence.entity.AmendeDefautDepotEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AmendeDefautDepotJpaRepository extends JpaRepository<AmendeDefautDepotEntity, Long> {

    Optional<AmendeDefautDepotEntity> findByIdObligationFiscale(Long idObligationFiscale);

    boolean existsByIdObligationFiscale(Long idObligationFiscale);

    List<AmendeDefautDepotEntity> findByNif(String nif);
}
