package mg.dgi.fiscaltrack.application.port.out;

import mg.dgi.fiscaltrack.domain.model.Notification;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface NotificationRepositoryPort {

    Notification save(Notification notification);

    Optional<Notification> findById(Long id);

    List<Notification> findAll();

    List<Notification> findByNif(String nif);

    List<Notification> findByStatutEnvoi(String statutEnvoi);

    List<Notification> findEnAttenteAvant(OffsetDateTime date);

    void deleteById(Long id);
}
