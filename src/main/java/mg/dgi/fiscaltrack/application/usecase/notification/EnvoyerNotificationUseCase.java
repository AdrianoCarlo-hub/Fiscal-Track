package mg.dgi.fiscaltrack.application.usecase.notification;

import mg.dgi.fiscaltrack.application.port.out.NotificationRepositoryPort;
import mg.dgi.fiscaltrack.application.port.out.NotificationSenderPort;
import mg.dgi.fiscaltrack.domain.enums.StatutEnvoi;
import mg.dgi.fiscaltrack.domain.model.Notification;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class EnvoyerNotificationUseCase {

    private final NotificationRepositoryPort notificationRepositoryPort;
    private final NotificationSenderPort notificationSenderPort;

    public EnvoyerNotificationUseCase(NotificationRepositoryPort notificationRepositoryPort,
                                       NotificationSenderPort notificationSenderPort) {
        this.notificationRepositoryPort = notificationRepositoryPort;
        this.notificationSenderPort = notificationSenderPort;
    }

    /**
     * RC18 : envoie toutes les notifications en attente dont la date d'envoi
     * prevue est passee. Chaque envoi est trace : ENVOYE ou ECHEC.
     */
    public int execute() {
        List<Notification> aEnvoyer = notificationRepositoryPort
                .findEnAttenteAvant(OffsetDateTime.now());

        int compteur = 0;
        for (Notification notification : aEnvoyer) {
            try {
                notificationSenderPort.envoyer(notification);
                notification.setStatutEnvoi(StatutEnvoi.ENVOYE);
                notification.setDateEnvoiEffective(OffsetDateTime.now());
                compteur++;
            } catch (Exception e) {
                notification.setStatutEnvoi(StatutEnvoi.ECHEC);
            }
            notificationRepositoryPort.save(notification);
        }
        return compteur;
    }

    public Notification envoyerUne(Long idNotification) {
        Notification notification = notificationRepositoryPort.findById(idNotification)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Notification introuvable : " + idNotification));

        if (notification.getStatutEnvoi() == StatutEnvoi.ENVOYE) {
            throw new IllegalArgumentException("Cette notification a deja ete envoyee");
        }

        try {
            notificationSenderPort.envoyer(notification);
            notification.setStatutEnvoi(StatutEnvoi.ENVOYE);
            notification.setDateEnvoiEffective(OffsetDateTime.now());
        } catch (Exception e) {
            notification.setStatutEnvoi(StatutEnvoi.ECHEC);
        }
        return notificationRepositoryPort.save(notification);
    }
}
