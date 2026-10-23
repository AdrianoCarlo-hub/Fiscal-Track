package mg.dgi.fiscaltrack.application.usecase.notification;

import mg.dgi.fiscaltrack.application.port.out.NotificationRepositoryPort;
import mg.dgi.fiscaltrack.domain.model.Notification;
import org.springframework.stereotype.Service;

/**
 * Marque comme lue une notification du portail appartenant au contribuable.
 * Les notifications e-mail et SMS ne sont pas concernees (le canal PORTAIL
 * est le seul a porter un etat de lecture).
 */
@Service
public class MarquerNotificationLueUseCase {

    private final NotificationRepositoryPort repositoryPort;

    public MarquerNotificationLueUseCase(NotificationRepositoryPort repositoryPort) {
        this.repositoryPort = repositoryPort;
    }

    public Notification execute(Long id) {
        Notification notification = repositoryPort.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Notification introuvable : " + id));
        notification.setLu(Boolean.TRUE);
        return repositoryPort.save(notification);
    }
}
