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
public class EmettreTitrePerceptionUseCase {

    private static final Logger logger = LoggerFactory.getLogger(EmettreTitrePerceptionUseCase.class);

    // TODO: confirmer le delai reel avec le Centre Fiscal
    private static final int DELAI_TITRE_JOURS = 30;

    private final CompteCourantFiscalRepositoryPort compteRepositoryPort;

    public EmettreTitrePerceptionUseCase(CompteCourantFiscalRepositoryPort compteRepositoryPort) {
        this.compteRepositoryPort = compteRepositoryPort;
    }

    /**
     * Passe en TITRE_EMIS les comptes NON_SOLDE ou PARTIEL dont l'echeance
     * de l'obligation liee est depassee depuis plus de DELAI_TITRE_JOURS jours.
     */
    @Transactional
    public int execute() {
        LocalDate seuil = LocalDate.now().minusDays(DELAI_TITRE_JOURS);
        List<CompteCourantFiscal> eligibles = compteRepositoryPort.findEligiblesTitrePerception(seuil);

        int compteur = 0;
        for (CompteCourantFiscal compte : eligibles) {
            try {
                compte.setStatutRecouvrement(StatutRecouvrement.TITRE_EMIS);
                compteRepositoryPort.save(compte);
                compteur++;
                logger.info("[RECOUVREMENT] Titre de perception emis pour compte {}", compte.getIdCompte());
            } catch (Exception e) {
                logger.error("[RECOUVREMENT] Erreur emission titre compte {} : {}",
                        compte.getIdCompte(), e.getMessage());
            }
        }
        return compteur;
    }
}
