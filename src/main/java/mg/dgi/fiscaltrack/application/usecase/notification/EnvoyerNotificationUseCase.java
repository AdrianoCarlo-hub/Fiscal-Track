package mg.dgi.fiscaltrack.application.usecase.notification;

import mg.dgi.fiscaltrack.application.port.out.NotificationRepositoryPort;
import mg.dgi.fiscaltrack.domain.enums.StatutEnvoi;
import mg.dgi.fiscaltrack.domain.model.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Mise en file d'envoi des notifications.
 *
 * L'envoi reel (e-mail, SMS) est asynchrone : cette classe ne fait qu'enfer
 * les notifications sur l'exécuteur dedie. Une panne d'envoi n'interrompt
 * ni le scheduler ni l'appelant ; le statut final (ENVOYE / ECHEC /
 * ECHEC_DEFINITIF) est mis a jour par NotificationEnvoiAsyncService.
 */
@Service
public class EnvoyerNotificationUseCase {

    private static final Logger logger = LoggerFactory.getLogger(EnvoyerNotificationUseCase.class);

    private final NotificationRepositoryPort notificationRepositoryPort;
    private final NotificationEnvoiAsyncService envoiAsyncService;
    private final NotificationParametresService parametres;
    private final Clock clock;

    public EnvoyerNotificationUseCase(NotificationRepositoryPort notificationRepositoryPort,
                                      NotificationEnvoiAsyncService envoiAsyncService,
                                      NotificationParametresService parametres,
                                      Clock clock) {
        this.notificationRepositoryPort = notificationRepositoryPort;
        this.envoiAsyncService = envoiAsyncService;
        this.parametres = parametres;
        this.clock = clock;
    }

    /**
     * Enfile toutes les notifications en attente dont la date d'envoi
     * prevue est passee. Le traitement reel est asynchrone.
     *
     * @return le nombre de notifications mises en file
     */
    public int execute() {
        OffsetDateTime maintenant = OffsetDateTime.now(clock);
        List<Notification> aEnvoyer = notificationRepositoryPort.findEnAttenteAvant(maintenant);

        int compteur = 0;
        for (Notification notification : aEnvoyer) {
            if (enfiler(notification)) {
                compteur++;
            }
        }
        return compteur;
    }

    /**
     * Envoi manuel d'une notification existante (bouton « Envoyer »).
     */
    public Notification envoyerUne(Long idNotification) {
        Notification notification = notificationRepositoryPort.findById(idNotification)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Notification introuvable : " + idNotification));

        if (notification.getStatutEnvoi() == StatutEnvoi.ENVOYE) {
            throw new IllegalArgumentException("Cette notification a déjà été envoyée");
        }

        notification.setStatutEnvoi(StatutEnvoi.EN_ATTENTE);
        notificationRepositoryPort.save(notification);
        filer(notification);
        return notification;
    }

    /**
     * Renvoi d'une notification en echec (ECHEC ou ECHEC_DEFINITIF).
     * Le compteur de tentatives est conserve pour la tracabilite.
     */
    public Notification renvoyer(Long idNotification) {
        Notification notification = notificationRepositoryPort.findById(idNotification)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Notification introuvable : " + idNotification));

        if (notification.getStatutEnvoi() != StatutEnvoi.ECHEC
                && notification.getStatutEnvoi() != StatutEnvoi.ECHEC_DEFINITIF) {
            throw new IllegalArgumentException(
                    "Seules les notifications en échec peuvent être renvoyées");
        }

        notification.setStatutEnvoi(StatutEnvoi.EN_ATTENTE);
        notification.setErreurEnvoi(null);
        notificationRepositoryPort.save(notification);
        filer(notification);
        return notification;
    }

    /**
     * Retente les notifications en echec encore retentables
     * (nb_tentatives < maximum), avec delai croissant respecte par le
     * scheduler dedie.
     */
    public int retenterEchecs() {
        List<Notification> echecs = notificationRepositoryPort
                .findEchecsRetentables(parametres.getNbTentativesMax());
        OffsetDateTime maintenant = OffsetDateTime.now(clock);
        int compteur = 0;
        for (Notification notification : echecs) {
            if (pretPourRetentative(notification, maintenant) && enfiler(notification)) {
                compteur++;
            }
        }
        return compteur;
    }

    private boolean pretPourRetentative(Notification notification, OffsetDateTime maintenant) {
        int tentatives = notification.getNbTentatives() == null ? 0 : notification.getNbTentatives();
        if (tentatives == 0) {
            return true;
        }
        int delaiMinutes = parametres.delaiAvantTentative(tentatives);
        OffsetDateTime derniere = notification.getDerniereTentative();
        return derniere == null || !derniere.plusMinutes(delaiMinutes).isAfter(maintenant);
    }

    /** Enfile en ignorant silencieusement un rejet de file (reste EN_ATTENTE). */
    private boolean enfiler(Notification notification) {
        try {
            filer(notification);
            return true;
        } catch (Exception e) {
            logger.warn("[NOTIFICATION][ENVOI] Mise en file impossible pour la notification #{} : {}",
                    notification.getIdNotif(), MessagesErreurEnvoi.courte(e));
            return false;
        }
    }

    private void filer(Notification notification) {
        envoiAsyncService.envoyerAsync(notification.getIdNotif());
    }
}
