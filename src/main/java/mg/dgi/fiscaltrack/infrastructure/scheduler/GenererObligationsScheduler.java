package mg.dgi.fiscaltrack.infrastructure.scheduler;

import mg.dgi.fiscaltrack.application.port.out.ContribuableRepositoryPort;
import mg.dgi.fiscaltrack.application.port.out.TypeImpotRepositoryPort;
import mg.dgi.fiscaltrack.application.usecase.obligation.GenererObligationsUseCase;
import mg.dgi.fiscaltrack.domain.enums.Periodicite;
import mg.dgi.fiscaltrack.domain.enums.StatutActivite;
import mg.dgi.fiscaltrack.domain.model.Contribuable;
import mg.dgi.fiscaltrack.domain.model.TypeImpot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class GenererObligationsScheduler {

    private static final Logger logger = LoggerFactory.getLogger(GenererObligationsScheduler.class);

    private final ContribuableRepositoryPort contribuableRepositoryPort;
    private final TypeImpotRepositoryPort typeImpotRepositoryPort;
    private final GenererObligationsUseCase genererObligationsUseCase;

    public GenererObligationsScheduler(ContribuableRepositoryPort contribuableRepositoryPort,
                                        TypeImpotRepositoryPort typeImpotRepositoryPort,
                                        GenererObligationsUseCase genererObligationsUseCase) {
        this.contribuableRepositoryPort = contribuableRepositoryPort;
        this.typeImpotRepositoryPort = typeImpotRepositoryPort;
        this.genererObligationsUseCase = genererObligationsUseCase;
    }

    /**
     * Le 1er de chaque mois a 2h : genere les obligations mensuelles
     * pour le mois precedent (periode fiscale au format YYYY-MM).
     */
    @Scheduled(cron = "0 0 2 1 * *")
    public void genererObligationsMensuelles() {
        String periode = calculerPeriodeMensuelle();
        logger.info("[SCHEDULER] Debut generation obligations MENSUELLE pour {}", periode);
        int compteur = genererPourPeriodicite(Periodicite.MENSUELLE, periode);
        logger.info("[SCHEDULER] {} obligations mensuelles generees", compteur);
    }

    /**
     * Le 1er janvier a 3h : genere les obligations annuelles
     * pour l'annee ecoulee (periode fiscale au format YYYY).
     */
    @Scheduled(cron = "0 0 3 1 1 *")
    public void genererObligationsAnnuelles() {
        String periode = String.valueOf(LocalDate.now().getYear() - 1);
        logger.info("[SCHEDULER] Debut generation obligations ANNUELLE pour {}", periode);
        int compteur = genererPourPeriodicite(Periodicite.ANNUELLE, periode);
        logger.info("[SCHEDULER] {} obligations annuelles generees", compteur);
    }

    /**
     * Methode publique pour declenchement manuel via SchedulerController.
     */
    public int declencherMensuelles() {
        return genererPourPeriodicite(Periodicite.MENSUELLE, calculerPeriodeMensuelle());
    }

    public int declencherAnnuelles() {
        return genererPourPeriodicite(Periodicite.ANNUELLE, String.valueOf(LocalDate.now().getYear() - 1));
    }

    private int genererPourPeriodicite(Periodicite periodicite, String periodeFiscale) {
        List<Contribuable> contribuables = contribuableRepositoryPort.findAll().stream()
                .filter(c -> c.getStatutActivite() == StatutActivite.ACTIF)
                .toList();

        List<TypeImpot> typesImpots = typeImpotRepositoryPort.findAll().stream()
                .filter(t -> t.getPeriodicite() == periodicite)
                .toList();

        int compteur = 0;
        for (Contribuable contribuable : contribuables) {
            for (TypeImpot typeImpot : typesImpots) {
                try {
                    genererObligationsUseCase.execute(
                            contribuable.getNif(),
                            typeImpot.getCodeImpot(),
                            periodeFiscale);
                    compteur++;
                } catch (IllegalArgumentException e) {
                    // Doublon deja existant : ignore silencieusement
                } catch (Exception e) {
                    logger.error("[SCHEDULER] Erreur generation {}/{} : {}",
                            contribuable.getNif(), typeImpot.getCodeImpot(), e.getMessage());
                }
            }
        }
        return compteur;
    }

    private String calculerPeriodeMensuelle() {
        LocalDate moisPrecedent = LocalDate.now().minusMonths(1);
        return String.format("%d-%02d", moisPrecedent.getYear(), moisPrecedent.getMonthValue());
    }
}
