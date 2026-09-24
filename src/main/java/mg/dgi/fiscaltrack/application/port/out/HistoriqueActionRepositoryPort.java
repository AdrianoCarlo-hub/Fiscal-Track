package mg.dgi.fiscaltrack.application.port.out;

import java.util.List;

public interface HistoriqueActionRepositoryPort {

    void enregistrer(String utilisateur, String action, String details);

    List<HistoriqueAction> findAll();

    List<HistoriqueAction> findByUtilisateur(String utilisateur);

    /**
     * Vue simplifiee pour l'exposition API.
     */
    record HistoriqueAction(Long idHistorique, String utilisateur, String action,
                             String details, java.time.OffsetDateTime createdAt) {}
}
