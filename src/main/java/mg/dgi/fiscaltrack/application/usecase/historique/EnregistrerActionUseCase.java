package mg.dgi.fiscaltrack.application.usecase.historique;

import mg.dgi.fiscaltrack.application.port.out.HistoriqueActionRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class EnregistrerActionUseCase {

    private static final Logger logger = LoggerFactory.getLogger(EnregistrerActionUseCase.class);

    private final HistoriqueActionRepositoryPort historiqueRepositoryPort;

    public EnregistrerActionUseCase(HistoriqueActionRepositoryPort historiqueRepositoryPort) {
        this.historiqueRepositoryPort = historiqueRepositoryPort;
    }

    /**
     * Enregistre une action utilisateur dans la table historique_actions.
     */
    public void execute(String utilisateur, String action, String details) {
        if (utilisateur == null || action == null) {
            logger.warn("[AUDIT] Tentative d'enregistrement sans utilisateur ou action");
            return;
        }
        try {
            historiqueRepositoryPort.enregistrer(utilisateur, action, details);
            logger.debug("[AUDIT] {} | {} | {}", utilisateur, action, details);
        } catch (Exception e) {
            logger.error("[AUDIT] Erreur enregistrement action : {}", e.getMessage());
        }
    }
}
