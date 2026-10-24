package mg.dgi.fiscaltrack.infrastructure.mail;

import mg.dgi.fiscaltrack.application.port.out.NotificationSenderPort;
import mg.dgi.fiscaltrack.domain.model.Notification;
import mg.dgi.fiscaltrack.domain.util.Masquage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Canal PORTAIL : la notification est deja stockee en base et consultable
 * dans l'espace du contribuable. Aucun envoi externe ; l'envoi est reussi
 * des que la ligne existe.
 */
@Component
public class PortailNotificationSenderAdapter implements NotificationSenderPort {

    private static final Logger logger = LoggerFactory.getLogger(PortailNotificationSenderAdapter.class);

    @Override
    public void envoyer(Notification notification) {
        logger.info("[NOTIFICATION][PORTAIL] Notification #{} publiée pour le NIF {}",
                notification.getIdNotif(), Masquage.masquerNif(notification.getNif()));
    }

    @Override
    public boolean supporterCanal(String canalEnvoi) {
        return "PORTAIL".equals(canalEnvoi);
    }
}
