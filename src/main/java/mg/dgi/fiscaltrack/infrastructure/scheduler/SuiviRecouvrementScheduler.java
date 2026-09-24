package mg.dgi.fiscaltrack.infrastructure.scheduler;

import mg.dgi.fiscaltrack.application.usecase.paiement.EmettreTitrePerceptionUseCase;
import mg.dgi.fiscaltrack.application.usecase.paiement.LancerAtdUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SuiviRecouvrementScheduler {

    private static final Logger logger = LoggerFactory.getLogger(SuiviRecouvrementScheduler.class);

    private final EmettreTitrePerceptionUseCase emettreTitrePerceptionUseCase;
    private final LancerAtdUseCase lancerAtdUseCase;

    public SuiviRecouvrementScheduler(EmettreTitrePerceptionUseCase emettreTitrePerceptionUseCase,
                                        LancerAtdUseCase lancerAtdUseCase) {
        this.emettreTitrePerceptionUseCase = emettreTitrePerceptionUseCase;
        this.lancerAtdUseCase = lancerAtdUseCase;
    }

    /**
     * Tous les jours a 10h : emission des titres de perception puis
     * lancement des ATD pour les comptes eligibles.
     */
    @Scheduled(cron = "0 0 10 * * *")
    public void executerSuiviRecouvrement() {
        logger.info("[SCHEDULER] Debut du suivi de recouvrement");
        try {
            int titresEmis = emettreTitrePerceptionUseCase.execute();
            logger.info("[SCHEDULER] {} titres de perception emis", titresEmis);

            int atdLances = lancerAtdUseCase.execute();
            logger.info("[SCHEDULER] {} ATD lances", atdLances);
        } catch (Exception e) {
            logger.error("[SCHEDULER] Erreur lors du suivi de recouvrement", e);
        }
    }
}
