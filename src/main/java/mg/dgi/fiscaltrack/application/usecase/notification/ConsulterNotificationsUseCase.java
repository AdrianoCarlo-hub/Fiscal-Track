package mg.dgi.fiscaltrack.application.usecase.notification;

import mg.dgi.fiscaltrack.application.port.out.NotificationRepositoryPort;
import mg.dgi.fiscaltrack.domain.model.Notification;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ConsulterNotificationsUseCase {

    private final NotificationRepositoryPort repositoryPort;

    public ConsulterNotificationsUseCase(NotificationRepositoryPort repositoryPort) {
        this.repositoryPort = repositoryPort;
    }

    public Notification parId(Long id) {
        return repositoryPort.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Notification introuvable : " + id));
    }

    public List<Notification> parContribuable(String nif) {
        return repositoryPort.findByNif(nif);
    }

    public List<Notification> parStatut(String statutEnvoi) {
        return repositoryPort.findByStatutEnvoi(statutEnvoi);
    }

    public List<Notification> toutes() {
        return repositoryPort.findAll();
    }
}
