package mg.dgi.fiscaltrack.infrastructure.persistence.repository;

import mg.dgi.fiscaltrack.infrastructure.persistence.entity.HistoriqueActionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistoriqueActionJpaRepository extends JpaRepository<HistoriqueActionEntity, Long> {

    List<HistoriqueActionEntity> findByUtilisateur(String utilisateur);

    List<HistoriqueActionEntity> findByAction(String action);

    List<HistoriqueActionEntity> findAllByOrderByCreatedAtDesc();
}
