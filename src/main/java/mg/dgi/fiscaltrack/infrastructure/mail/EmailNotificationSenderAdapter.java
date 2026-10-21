package mg.dgi.fiscaltrack.infrastructure.mail;

import mg.dgi.fiscaltrack.application.port.out.ContribuableRepositoryPort;
import mg.dgi.fiscaltrack.application.port.out.NotificationSenderPort;
import mg.dgi.fiscaltrack.domain.model.Contribuable;
import mg.dgi.fiscaltrack.domain.model.Notification;
import mg.dgi.fiscaltrack.domain.util.Masquage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Envoi reel des notifications par e-mail (Spring Mail / SMTP).
 *
 * Configuration (variables d'environnement) :
 *   NOTIFICATIONS_EMAIL_ENABLED : true pour activer l'envoi reel
 *   MAIL_HOST, MAIL_PORT, MAIL_USERNAME, MAIL_PASSWORD, MAIL_EXPEDITEUR
 *
 * Si l'envoi est desactive ou si la configuration manque, la notification
 * est journalisee (mode simulation) et consideree comme traitee : l'application
 * demarre et tourne sans aucun identifiant.
 */
@Component
public class EmailNotificationSenderAdapter implements NotificationSenderPort {

    private static final Logger logger = LoggerFactory.getLogger(EmailNotificationSenderAdapter.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final ContribuableRepositoryPort contribuableRepositoryPort;
    private final boolean envoiReelActive;
    private final boolean smtpConfigure;
    private final String expediteur;

    public EmailNotificationSenderAdapter(ObjectProvider<JavaMailSender> mailSenderProvider,
                                          ContribuableRepositoryPort contribuableRepositoryPort,
                                          @Value("${NOTIFICATIONS_EMAIL_ENABLED:false}") boolean envoiReelActive,
                                          @Value("${spring.mail.host:}") String mailHost,
                                          @Value("${MAIL_EXPEDITEUR:no-reply@fiscaltrack.mg}") String expediteur) {
        this.mailSenderProvider = mailSenderProvider;
        this.contribuableRepositoryPort = contribuableRepositoryPort;
        this.envoiReelActive = envoiReelActive;
        this.smtpConfigure = mailHost != null && !mailHost.isBlank();
        this.expediteur = expediteur;
    }

    @Override
    public void envoyer(Notification notification) {
        if (!envoiReelActive || !smtpConfigure) {
            logger.info("[NOTIFICATION][EMAIL][SIMULATION] Notification #{} NIF {} | Sujet: {}",
                    notification.getIdNotif(),
                    Masquage.masquerNif(notification.getNif()),
                    premierLigne(notification));
            return;
        }

        Optional<String> destinataire = contribuableRepositoryPort.findByNif(notification.getNif())
                .map(Contribuable::getEmailContribuable)
                .filter(email -> email != null && !email.isBlank());
        if (destinataire.isEmpty()) {
            throw new IllegalStateException("Adresse e-mail non renseignée pour ce contribuable");
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            throw new IllegalStateException("Service d'envoi d'e-mail indisponible");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(expediteur);
        message.setTo(destinataire.get());
        message.setSubject(sujet(notification));
        message.setText(notification.getMessageContenu());
        mailSender.send(message);

        logger.info("[NOTIFICATION][EMAIL][REEL] Notification #{} envoyée",
                notification.getIdNotif());
    }

    private String sujet(Notification notification) {
        String type = notification.getTypeRelance() == null
                ? "Notification"
                : notification.getTypeRelance().getCode().replace('_', ' ');
        return "Fiscal-Track - " + type;
    }

    private String premierLigne(Notification notification) {
        String contenu = notification.getMessageContenu();
        if (contenu == null) {
            return "";
        }
        int retour = contenu.indexOf('\n');
        return retour < 0 ? contenu : contenu.substring(0, retour);
    }

    @Override
    public boolean supporterCanal(String canalEnvoi) {
        return "EMAIL".equals(canalEnvoi);
    }
}
