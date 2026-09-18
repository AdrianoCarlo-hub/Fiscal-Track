package mg.dgi.fiscaltrack.infrastructure.persistence.repository;

import mg.dgi.fiscaltrack.infrastructure.persistence.entity.CompteCourantFiscalEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompteCourantFiscalJpaRepository extends JpaRepository<CompteCourantFiscalEntity, Long> {

    Optional<CompteCourantFiscalEntity> findByIdDeclaration(Long idDeclaration);

    List<CompteCourantFiscalEntity> findByNif(String nif);

    List<CompteCourantFiscalEntity> findByStatutRecouvrement(String statutRecouvrement);

    @Query("SELECT c FROM CompteCourantFiscalEntity c WHERE c.statutRecouvrement <> 'SOLDE'")
    List<CompteCourantFiscalEntity> findNonSoldes();
}
