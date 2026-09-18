package mg.dgi.fiscaltrack.infrastructure.scheduler;

import mg.dgi.fiscaltrack.application.usecase.notification.EnvoyerNotificationUseCase;
import mg.dgi.fiscaltrack.application.usecase.obligation.IdentifierRetardsUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DetectionRetardScheduler {

    private static final Logger logger = LoggerFactory.getLogger(DetectionRetardScheduler.class);

    private final IdentifierRetardsUseCase identifierRetardsUseCase;
    private final EnvoyerNotificationUseCase envoyerNotificationUseCase;

    public DetectionRetardScheduler(IdentifierRetardsUseCase identifierRetardsUseCase,
                                     EnvoyerNotificationUseCase envoyerNotificationUseCase) {
        this.identifierRetardsUseCase = identifierRetardsUseCase;
        this.envoyerNotificationUseCase = envoyerNotificationUseCase;
    }

    /**
     * Tous les jours a 9h00 : identifie les obligations en retard,
     * passe leur statut a RETARD et envoie les notifications associees.
     */
    @Scheduled(cron = "0 0 9 * * *")
    public void executerDetectionRetards() {
        logger.info("[SCHEDULER] Debut de la detection des retards");

        try {
            int nombreRetards = identifierRetardsUseCase.execute().size();
            logger.info("[SCHEDULER] {} obligations passees en RETARD", nombreRetards);

            int envoyes = envoyerNotificationUseCase.execute();
            logger.info("[SCHEDULER] {} notifications de retard envoyees", envoyes);
        } catch (Exception e) {
            logger.error("[SCHEDULER] Erreur lors de la detection des retards", e);
        }
    }
}
