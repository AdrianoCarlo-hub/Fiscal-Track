package mg.dgi.fiscaltrack.infrastructure.persistence.repository;

import mg.dgi.fiscaltrack.infrastructure.persistence.entity.NotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface NotificationJpaRepository extends JpaRepository<NotificationEntity, Long> {

    List<NotificationEntity> findByNif(String nif);

    List<NotificationEntity> findByStatutEnvoi(String statutEnvoi);

    List<NotificationEntity> findByStatutEnvoiAndDateEnvoiPrevueBefore(String statutEnvoi,
                                                                      OffsetDateTime date);
}
