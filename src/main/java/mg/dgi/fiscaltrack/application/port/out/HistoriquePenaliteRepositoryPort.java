package mg.dgi.fiscaltrack.application.port.out;

import mg.dgi.fiscaltrack.domain.model.HistoriquePenalite;

import java.util.List;

public interface HistoriquePenaliteRepositoryPort {

    HistoriquePenalite save(HistoriquePenalite ligne);

    List<HistoriquePenalite> findByIdCompte(Long idCompte);

    List<HistoriquePenalite> findByIdCompteAndTypeLigne(Long idCompte, String typeLigne);
}
