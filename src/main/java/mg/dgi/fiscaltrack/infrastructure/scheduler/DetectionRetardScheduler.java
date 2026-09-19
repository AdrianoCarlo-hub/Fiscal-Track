package mg.dgi.fiscaltrack.infrastructure.scheduler;

import mg.dgi.fiscaltrack.application.port.out.CompteCourantFiscalRepositoryPort;
import mg.dgi.fiscaltrack.application.port.out.DeclarationRepositoryPort;
import mg.dgi.fiscaltrack.application.usecase.notification.EnvoyerNotificationUseCase;
import mg.dgi.fiscaltrack.application.usecase.obligation.IdentifierRetardsUseCase;
import mg.dgi.fiscaltrack.application.usecase.penalite.CalculerPenaliteUseCase;
import mg.dgi.fiscaltrack.domain.model.ObligationFiscale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DetectionRetardScheduler {

    private static final Logger logger = LoggerFactory.getLogger(DetectionRetardScheduler.class);

    private final IdentifierRetardsUseCase identifierRetardsUseCase;
    private final EnvoyerNotificationUseCase envoyerNotificationUseCase;
    private final CalculerPenaliteUseCase calculerPenaliteUseCase;
    private final DeclarationRepositoryPort declarationRepositoryPort;
    private final CompteCourantFiscalRepositoryPort compteCourantFiscalRepositoryPort;

    public DetectionRetardScheduler(IdentifierRetardsUseCase identifierRetardsUseCase,
                                     EnvoyerNotificationUseCase envoyerNotificationUseCase,
                                     CalculerPenaliteUseCase calculerPenaliteUseCase,
                                     DeclarationRepositoryPort declarationRepositoryPort,
                                     CompteCourantFiscalRepositoryPort compteCourantFiscalRepositoryPort) {
        this.identifierRetardsUseCase = identifierRetardsUseCase;
        this.envoyerNotificationUseCase = envoyerNotificationUseCase;
        this.calculerPenaliteUseCase = calculerPenaliteUseCase;
        this.declarationRepositoryPort = declarationRepositoryPort;
        this.compteCourantFiscalRepositoryPort = compteCourantFiscalRepositoryPort;
    }

    /**
     * Tous les jours a 9h00 : identifie les obligations en retard,
     * passe leur statut a RETARD, recalcule les penalites (RC4-RC7)
     * puis envoie les notifications associees.
     */
    @Scheduled(cron = "0 0 9 * * *")
    public void executerDetectionRetards() {
        logger.info("[SCHEDULER] Debut de la detection des retards");

        try {
            List<ObligationFiscale> obligationsEnRetard = identifierRetardsUseCase.execute();
            logger.info("[SCHEDULER] {} obligations passees en RETARD", obligationsEnRetard.size());

            int penalitesCalculees = appliquerPenalites(obligationsEnRetard);
            logger.info("[SCHEDULER] {} penalites recalculees (RC4-RC7)", penalitesCalculees);

            int envoyes = envoyerNotificationUseCase.execute();
            logger.info("[SCHEDULER] {} notifications de retard envoyees", envoyes);
        } catch (Exception e) {
            logger.error("[SCHEDULER] Erreur lors de la detection des retards", e);
        }
    }

    /**
     * RC4-RC7 : apres avoir marque une obligation en RETARD, on recalcule
     * la penalite sur le compte courant fiscal associe.
     * Chaine : obligation -> declaration -> compte courant.
     * Une obligation sans declaration deposee n'a pas de compte courant,
     * elle est ignoree silencieusement.
     */
    private int appliquerPenalites(List<ObligationFiscale> obligations) {
        int compteur = 0;
        for (ObligationFiscale obligation : obligations) {
            try {
                var declarationOpt = declarationRepositoryPort
                        .findByIdObligationFiscale(obligation.getIdObligationFiscale());
                if (declarationOpt.isEmpty()) {
                    continue;
                }
                var compteOpt = compteCourantFiscalRepositoryPort
                        .findByIdDeclaration(declarationOpt.get().getIdDeclaration());
                if (compteOpt.isEmpty()) {
                    continue;
                }
                calculerPenaliteUseCase.execute(
                        compteOpt.get().getIdCompte(),
                        obligation.getDateLimiteReelle());
                compteur++;
            } catch (Exception e) {
                logger.error("[SCHEDULER] Erreur penalite pour obligation {} : {}",
                        obligation.getIdObligationFiscale(), e.getMessage());
            }
        }
        return compteur;
    }
}
