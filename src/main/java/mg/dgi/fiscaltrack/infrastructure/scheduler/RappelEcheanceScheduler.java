package mg.dgi.fiscaltrack.infrastructure.scheduler;

import mg.dgi.fiscaltrack.application.usecase.notification.EnvoyerNotificationUseCase;
import mg.dgi.fiscaltrack.application.usecase.notification.GenererRappelUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RappelEcheanceScheduler {

    private static final Logger logger = LoggerFactory.getLogger(RappelEcheanceScheduler.class);

    private final GenererRappelUseCase genererRappelUseCase;
    private final EnvoyerNotificationUseCase envoyerNotificationUseCase;

    public RappelEcheanceScheduler(GenererRappelUseCase genererRappelUseCase,
                                    EnvoyerNotificationUseCase envoyerNotificationUseCase) {
        this.genererRappelUseCase = genererRappelUseCase;
        this.envoyerNotificationUseCase = envoyerNotificationUseCase;
    }

    /**
     * Tous les jours a 8h00 : genere les rappels J-7 et J-1,
     * puis envoie toutes les notifications en attente.
     */
    @Scheduled(cron = "0 0 8 * * *")
    public void executerRappelsQuotidiens() {
        logger.info("[SCHEDULER] Debut de la generation des rappels quotidiens");

        try {
            int rappelsJ7 = genererRappelUseCase.execute(7).size();
            logger.info("[SCHEDULER] {} rappels J-7 generes", rappelsJ7);

            int rappelsJ1 = genererRappelUseCase.execute(1).size();
            logger.info("[SCHEDULER] {} rappels J-1 generes", rappelsJ1);

            int envoyes = envoyerNotificationUseCase.execute();
            logger.info("[SCHEDULER] {} notifications envoyees", envoyes);
        } catch (Exception e) {
            logger.error("[SCHEDULER] Erreur lors des rappels quotidiens", e);
        }
    }
}
