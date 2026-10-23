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
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.MimeMessageHelper;
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
    private final String urlPortail;

    public EmailNotificationSenderAdapter(ObjectProvider<JavaMailSender> mailSenderProvider,
                                          ContribuableRepositoryPort contribuableRepositoryPort,
                                          @Value("${NOTIFICATIONS_EMAIL_ENABLED:false}") boolean envoiReelActive,
                                          @Value("${spring.mail.host:}") String mailHost,
                                          @Value("${MAIL_EXPEDITEUR:no-reply@fiscaltrack.mg}") String expediteur,
                                          @Value("${app.portal.url:http://localhost:5173}") String urlPortail) {
        this.mailSenderProvider = mailSenderProvider;
        this.contribuableRepositoryPort = contribuableRepositoryPort;
        this.envoiReelActive = envoiReelActive;
        this.smtpConfigure = mailHost != null && !mailHost.isBlank();
        this.expediteur = expediteur;
        this.urlPortail = urlPortail;
    }

    @Override
    public void envoyer(Notification notification) {
        if (!envoiReelActive || !smtpConfigure) {
            logger.info("[NOTIFICATION][EMAIL][SIMULATION] Notification #{} NIF {} | Sujet: {}",
                    notification.getIdNotif(),
                    Masquage.masquerNif(notification.getNif()),
                    sujet(notification));
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

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(expediteur);
            helper.setTo(destinataire.get());
            helper.setSubject(sujet(notification));
            helper.setText(notification.getMessageContenu(), corpsHtml(notification));
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new IllegalStateException("Construction de l'e-mail impossible", e);
        }

        logger.info("[NOTIFICATION][EMAIL][REEL] Notification #{} envoyée",
                notification.getIdNotif());
    }

    private String sujet(Notification notification) {
        if (notification.getObjetEmail() != null && !notification.getObjetEmail().isBlank()) {
            return notification.getObjetEmail();
        }
        String type = notification.getTypeRelance() == null
                ? "Notification"
                : notification.getTypeRelance().getCode().replace('_', ' ');
        return "Fiscal-Track - " + type;
    }

    /** Version HTML : corps en texte (lignes conservees) + bouton d'acces au portail. */
    private String corpsHtml(Notification notification) {
        String corps = notification.getMessageContenu() == null ? "" : notification.getMessageContenu();
        String echappe = corps
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\n", "<br>");
        return "<div style=\"font-family:Georgia,serif;color:#1E2761;max-width:600px\">"
                + "<p>" + echappe + "</p>"
                + "<p style=\"margin:28px 0\">"
                + "<a href=\"" + urlPortail + "\" "
                + "style=\"background:#1E2761;color:#FFFFFF;padding:12px 22px;border-radius:6px;"
                + "text-decoration:none;font-family:Arial,sans-serif\">"
                + "Accéder à mon espace</a></p>"
                + "<p style=\"font-size:12px;color:#6B7280\">Fiscal-Track — Direction Générale des Impôts</p>"
                + "</div>";
    }

    @Override
    public boolean supporterCanal(String canalEnvoi) {
        return "EMAIL".equals(canalEnvoi);
    }
}
