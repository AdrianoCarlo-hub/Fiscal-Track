package mg.dgi.fiscaltrack.application.usecase.notification;

import mg.dgi.fiscaltrack.application.port.out.ContribuableRepositoryPort;
import mg.dgi.fiscaltrack.application.port.out.NotificationRepositoryPort;
import mg.dgi.fiscaltrack.domain.enums.CanalEnvoi;
import mg.dgi.fiscaltrack.domain.enums.StatutEnvoi;
import mg.dgi.fiscaltrack.domain.model.Contribuable;
import mg.dgi.fiscaltrack.domain.model.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * Envoi asynchrone d'une notification, avec tracabilite des tentatives.
 *
 * - Une panne d'envoi n'interrompt jamais l'appelant : l'erreur est
 *   capturee dans la colonne erreur_envoi (message court, sans secret).
 * - Apres NOTIFICATIONS_MAX_TENTATIVES tentatives, le statut passe a
 *   ECHEC_DEFINITIF (renvoi manuel toujours possible).
 */
@Service
public class NotificationEnvoiAsyncService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationEnvoiAsyncService.class);

    private final NotificationRepositoryPort notificationRepositoryPort;
    private final ContribuableRepositoryPort contribuableRepositoryPort;
    private final NotificationDispatchService dispatchService;
    private final CanalNotificationService canalNotificationService;
    private final NotificationParametresService parametres;
    private final Clock clock;

    public NotificationEnvoiAsyncService(NotificationRepositoryPort notificationRepositoryPort,
                                         ContribuableRepositoryPort contribuableRepositoryPort,
                                         NotificationDispatchService dispatchService,
                                         CanalNotificationService canalNotificationService,
                                         NotificationParametresService parametres,
                                         Clock clock) {
        this.notificationRepositoryPort = notificationRepositoryPort;
        this.contribuableRepositoryPort = contribuableRepositoryPort;
        this.dispatchService = dispatchService;
        this.canalNotificationService = canalNotificationService;
        this.parametres = parametres;
        this.clock = clock;
    }

    /**
     * Enfile la notification sur l'exécuteur dedie. Si la file est pleine,
     * la notification reste EN_ATTENTE et aucune exception n'est remontee.
     */
    @Async("notificationTaskExecutor")
    public void envoyerAsync(Long idNotification) {
        Notification notification = notificationRepositoryPort.findById(idNotification)
                .orElse(null);
        if (notification == null) {
            logger.warn("[NOTIFICATION][ENVOI] Notification #{} introuvable", idNotification);
            return;
        }
        envoyerMaintenant(notification);
    }

    /**
     * Envoi synchrone (utilise par les tests et les cas ou l'agent attend
     * le resultat, comme l'envoi de test de configuration).
     */
    public Notification envoyerMaintenant(Notification notification) {
        String nifSecurise = masquerNif(notification.getNif());
        try {
            Optional<Contribuable> contribuable =
                    contribuableRepositoryPort.findByNif(notification.getNif());

            if (contribuable.isPresent()
                    && canalNotificationService.refuse(contribuable.get(), notification.getCanalEnvoi())) {
                notification.setStatutEnvoi(StatutEnvoi.IGNORE_PREFERENCE);
                notification.setErreurEnvoi(null);
                notificationRepositoryPort.save(notification);
                logger.info("[NOTIFICATION][ENVOI][IGNORE] NIF {} canal {} : preference du contribuable",
                        nifSecurise, notification.getCanalEnvoi());
                return notification;
            }

            String indisponibilite = verifierCoordonnee(contribuable.orElse(null), notification);
            if (indisponibilite != null) {
                marquerEchec(notification, indisponibilite);
                return notification;
            }

            notification.setNbTentatives(
                    (notification.getNbTentatives() == null ? 0 : notification.getNbTentatives()) + 1);
            notification.setDerniereTentative(OffsetDateTime.now(clock));

            dispatchService.envoyer(notification);

            notification.setStatutEnvoi(StatutEnvoi.ENVOYE);
            notification.setDateEnvoiEffective(OffsetDateTime.now(clock));
            notification.setErreurEnvoi(null);
            notificationRepositoryPort.save(notification);
            logger.info("[NOTIFICATION][ENVOI][OK] #{} NIF {} canal {} tentative {}/{}",
                    notification.getIdNotif(), nifSecurise, notification.getCanalEnvoi(),
                    notification.getNbTentatives(), parametres.getNbTentativesMax());
        } catch (Exception e) {
            marquerEchec(notification, MessagesErreurEnvoi.courte(e));
            logger.warn("[NOTIFICATION][ENVOI][ECHEC] #{} NIF {} canal {} : {}",
                    notification.getIdNotif(), nifSecurise, notification.getCanalEnvoi(),
                    notification.getErreurEnvoi());
        }
        return notification;
    }

    /** Message si la coordonnee du canal est absente, sinon null. */
    private String verifierCoordonnee(Contribuable contribuable, Notification notification) {
        if (notification.getCanalEnvoi() == CanalEnvoi.EMAIL
                && !canalNotificationService.estEmailUtilisable(contribuable)) {
            return "Adresse e-mail non renseignée pour ce contribuable";
        }
        if (notification.getCanalEnvoi() == CanalEnvoi.SMS
                && !canalNotificationService.estSmsUtilisable(contribuable)) {
            return "Numéro de téléphone non renseigné pour ce contribuable";
        }
        return null;
    }

    private void marquerEchec(Notification notification, String erreurCourte) {
        int tentatives = (notification.getNbTentatives() == null ? 0 : notification.getNbTentatives());
        notification.setStatutEnvoi(tentatives >= parametres.getNbTentativesMax()
                ? StatutEnvoi.ECHEC_DEFINITIF
                : StatutEnvoi.ECHEC);
        notification.setErreurEnvoi(erreurCourte);
        notificationRepositoryPort.save(notification);
    }

    /** NIF masque pour les journaux (voir domain.util.Masquage). */
    public static String masquerNif(String nif) {
        return mg.dgi.fiscaltrack.domain.util.Masquage.masquerNif(nif);
    }
}
