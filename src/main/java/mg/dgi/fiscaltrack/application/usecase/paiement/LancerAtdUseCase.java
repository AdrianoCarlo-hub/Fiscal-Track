package mg.dgi.fiscaltrack.application.usecase.paiement;

import mg.dgi.fiscaltrack.application.port.out.CompteCourantFiscalRepositoryPort;
import mg.dgi.fiscaltrack.domain.enums.StatutRecouvrement;
import mg.dgi.fiscaltrack.domain.model.CompteCourantFiscal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class LancerAtdUseCase {

    private static final Logger logger = LoggerFactory.getLogger(LancerAtdUseCase.class);

    // TODO: confirmer le delai reel avec le Centre Fiscal
    private static final int DELAI_ATD_JOURS = 15;

    private final CompteCourantFiscalRepositoryPort compteRepositoryPort;

    public LancerAtdUseCase(CompteCourantFiscalRepositoryPort compteRepositoryPort) {
        this.compteRepositoryPort = compteRepositoryPort;
    }

    /**
     * Passe en ATD_LANCE les comptes en TITRE_EMIS depuis plus de
     * DELAI_ATD_JOURS jours (mesure via updated_at).
     */
    @Transactional
    public int execute() {
        LocalDate seuil = LocalDate.now().minusDays(DELAI_ATD_JOURS);
        List<CompteCourantFiscal> eligibles = compteRepositoryPort.findEligiblesAtd(seuil);

        int compteur = 0;
        for (CompteCourantFiscal compte : eligibles) {
            try {
                compte.setStatutRecouvrement(StatutRecouvrement.ATD_LANCE);
                compteRepositoryPort.save(compte);
                compteur++;
                logger.info("[RECOUVREMENT] ATD lance pour compte {}", compte.getIdCompte());
            } catch (Exception e) {
                logger.error("[RECOUVREMENT] Erreur lancement ATD compte {} : {}",
                        compte.getIdCompte(), e.getMessage());
            }
        }
        return compteur;
    }
}
