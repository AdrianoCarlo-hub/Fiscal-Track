package mg.dgi.fiscaltrack.infrastructure.mail;

import mg.dgi.fiscaltrack.application.port.out.ContribuableRepositoryPort;
import mg.dgi.fiscaltrack.application.port.out.NotificationSenderPort;
import mg.dgi.fiscaltrack.domain.enums.CanalEnvoi;
import mg.dgi.fiscaltrack.domain.model.Contribuable;
import mg.dgi.fiscaltrack.domain.model.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Envoi des notifications.
 *
 * - Canal EMAIL : envoi SMTP reel via JavaMailSender quand la configuration
 *   mail (spring.mail.host) est renseignee ; sinon repli sur le log.
 *   L'adresse du destinataire est resolue depuis le contribuable (NIF) ;
 *   si elle est absente, repli sur le log.
 * - Canal PORTAIL : notification deja consultable dans le portail du
 *   contribuable (stockee en base), aucun envoi externe.
 * - Canal SMS : HORS PERIMETRE (aucun fournisseur configure) ; la
 *   notification est journalisee et non envoyee.
 */
@Component
public class EmailNotificationSenderAdapter implements NotificationSenderPort {

    private static final Logger logger = LoggerFactory.getLogger(EmailNotificationSenderAdapter.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final ContribuableRepositoryPort contribuableRepositoryPort;
    private final boolean smtpConfigure;
    private final String expediteur;

    public EmailNotificationSenderAdapter(ObjectProvider<JavaMailSender> mailSenderProvider,
                                          ContribuableRepositoryPort contribuableRepositoryPort,
                                          @Value("${spring.mail.host:}") String mailHost,
                                          @Value("${fiscaltrack.mail.expediteur:no-reply@fiscaltrack.mg}") String expediteur) {
        this.mailSenderProvider = mailSenderProvider;
        this.contribuableRepositoryPort = contribuableRepositoryPort;
        this.smtpConfigure = mailHost != null && !mailHost.isBlank();
        this.expediteur = expediteur;
    }

    @Override
    public void envoyer(Notification notification) {
        CanalEnvoi canal = notification.getCanalEnvoi();

        if (canal == CanalEnvoi.SMS) {
            logger.warn("[NOTIFICATION][SMS][HORS PERIMETRE] Destinataire={} | Message={}",
                    notification.getNif(), notification.getMessageContenu());
            return;
        }

        if (canal == CanalEnvoi.PORTAIL) {
            logger.info("[NOTIFICATION][PORTAIL] Notification #{} disponible pour le NIF {}",
                    notification.getIdNotif(), notification.getNif());
            return;
        }

        if (!smtpConfigure) {
            logger.info("[NOTIFICATION][EMAIL][SMTP non configure - repli log] Destinataire={} | Type={} | Message={}",
                    notification.getNif(),
                    notification.getTypeRelance() == null ? null : notification.getTypeRelance().getCode(),
                    notification.getMessageContenu());
            return;
        }

        Optional<String> destinataire = contribuableRepositoryPort.findByNif(notification.getNif())
                .map(Contribuable::getEmailContribuable)
                .filter(email -> email != null && !email.isBlank());
        if (destinataire.isEmpty()) {
            logger.warn("[NOTIFICATION][EMAIL][adresse introuvable - repli log] NIF={} | Message={}",
                    notification.getNif(), notification.getMessageContenu());
            return;
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            logger.warn("[NOTIFICATION][EMAIL][JavaMailSender indisponible - repli log] NIF={}",
                    notification.getNif());
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(expediteur);
        message.setTo(destinataire.get());
        message.setSubject("Fiscal-Track - " + libelleType(notification));
        message.setText(notification.getMessageContenu());
        mailSender.send(message);

        logger.info("[NOTIFICATION][EMAIL] Envoyee au NIF {} ({})",
                notification.getNif(), destinataire.get());
    }

    private String libelleType(Notification notification) {
        return notification.getTypeRelance() == null
                ? "Notification"
                : notification.getTypeRelance().getCode();
    }

    @Override
    public boolean supporterCanal(String canalEnvoi) {
        return "EMAIL".equals(canalEnvoi);
    }
}
