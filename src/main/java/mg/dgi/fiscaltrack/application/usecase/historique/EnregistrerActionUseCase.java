package mg.dgi.fiscaltrack.application.usecase.historique;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
public class EnregistrerActionUseCase {

    private static final Logger logger = LoggerFactory.getLogger(EnregistrerActionUseCase.class);

    /**
     * Enregistre une action utilisateur dans les logs applicatifs.
     * Permet de tracer qui a fait quoi et quand (F56-F57).
     */
    public void execute(String utilisateur, String action, String details) {
        if (utilisateur == null || action == null) {
            throw new IllegalArgumentException("L'utilisateur et l'action sont obligatoires");
        }
        String message = String.format("[AUDIT] %s | utilisateur=%s | action=%s | details=%s",
                OffsetDateTime.now(), utilisateur, action, details != null ? details : "-");
        logger.info(message);
    }
}
