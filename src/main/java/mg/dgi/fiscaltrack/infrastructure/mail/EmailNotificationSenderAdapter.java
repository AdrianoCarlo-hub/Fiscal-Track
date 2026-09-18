package mg.dgi.fiscaltrack.infrastructure.mail;

import mg.dgi.fiscaltrack.application.port.out.NotificationSenderPort;
import mg.dgi.fiscaltrack.domain.model.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class EmailNotificationSenderAdapter implements NotificationSenderPort {

    private static final Logger logger = LoggerFactory.getLogger(EmailNotificationSenderAdapter.class);

    /**
     * Implementation minimale : log la notification au lieu d'envoyer un vrai
     * email. A remplacer par un vrai client SMTP quand la configuration mail
     * sera en place.
     */
    @Override
    public void envoyer(Notification notification) {
        logger.info("[NOTIFICATION] Canal={} | Destinataire={} | Type={} | Message={}",
                notification.getCanalEnvoi(),
                notification.getNif(),
                notification.getTypeRelance().getCode(),
                notification.getMessageContenu());
    }

    @Override
    public boolean supporterCanal(String canalEnvoi) {
        return "EMAIL".equals(canalEnvoi) || "SMS".equals(canalEnvoi) || "PORTAIL".equals(canalEnvoi);
    }
}
