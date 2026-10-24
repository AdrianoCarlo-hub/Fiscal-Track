package mg.dgi.fiscaltrack.infrastructure.scheduler;

import mg.dgi.fiscaltrack.application.usecase.notification.EnvoyerNotificationUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Retentatives automatiques des notifications en echec.
 *
 * Toutes les 5 minutes (heure de Madagascar) : refile les notifications
 * en ECHEC dont le nombre de tentatives est inferieur au maximum et dont
 * le delai de croissance (5 min, 30 min...) est ecoule. Au-dela du
 * maximum, le statut ECHEC_DEFINITIF est deja pose par l'envoi asynchrone
 * et le renvoi manuel reste possible.
 */
@Component
public class NouvellesTentativesScheduler {

    private static final Logger logger = LoggerFactory.getLogger(NouvellesTentativesScheduler.class);

    private final EnvoyerNotificationUseCase envoyerNotificationUseCase;

    public NouvellesTentativesScheduler(EnvoyerNotificationUseCase envoyerNotificationUseCase) {
        this.envoyerNotificationUseCase = envoyerNotificationUseCase;
    }

    @Scheduled(cron = "0 */5 * * * *", zone = "Indian/Antananarivo")
    public void executerRetentatives() {
        try {
            int refiles = envoyerNotificationUseCase.retenterEchecs();
            if (refiles > 0) {
                logger.info("[SCHEDULER] {} notification(s) en echec refilee(s) pour retentative", refiles);
            }
        } catch (Exception e) {
            logger.error("[SCHEDULER] Erreur lors des retentatives de notifications", e);
        }
    }
}
