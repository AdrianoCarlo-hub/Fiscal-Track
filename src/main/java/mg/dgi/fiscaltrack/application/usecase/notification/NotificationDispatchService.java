package mg.dgi.fiscaltrack.application.usecase.notification;

import mg.dgi.fiscaltrack.application.port.out.NotificationSenderPort;
import mg.dgi.fiscaltrack.domain.model.Notification;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Selection de l'adaptateur d'envoi en fonction du canal de la notification.
 * Seule depend a l'application : le port NotificationSenderPort.
 */
@Service
public class NotificationDispatchService {

    private final List<NotificationSenderPort> senders;

    public NotificationDispatchService(List<NotificationSenderPort> senders) {
        this.senders = senders;
    }

    public void envoyer(Notification notification) {
        String canal = notification.getCanalEnvoi() == null
                ? null
                : notification.getCanalEnvoi().name();
        NotificationSenderPort sender = senders.stream()
                .filter(s -> s.supporterCanal(canal))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Aucun adaptateur d'envoi pour le canal : " + canal));
        sender.envoyer(notification);
    }

    /** Vrai si un adaptateur est disponible pour ce canal (test de configuration). */
    public boolean peutEnvoyer(String canal) {
        return senders.stream().anyMatch(s -> s.supporterCanal(canal));
    }
}
