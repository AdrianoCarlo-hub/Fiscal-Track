package mg.dgi.fiscaltrack.infrastructure.persistence.repository;

import mg.dgi.fiscaltrack.infrastructure.persistence.entity.HistoriquePenaliteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistoriquePenaliteJpaRepository extends JpaRepository<HistoriquePenaliteEntity, Long> {

    List<HistoriquePenaliteEntity> findByIdCompteOrderByDateCalculAsc(Long idCompte);

    List<HistoriquePenaliteEntity> findByIdCompteAndTypeLigne(Long idCompte, String typeLigne);
}
