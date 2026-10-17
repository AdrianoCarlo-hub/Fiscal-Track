package mg.dgi.fiscaltrack.infrastructure.persistence.mapper;

import mg.dgi.fiscaltrack.domain.model.HistoriquePenalite;
import mg.dgi.fiscaltrack.infrastructure.persistence.entity.HistoriquePenaliteEntity;
import org.springframework.stereotype.Component;

@Component
public class HistoriquePenaliteMapper {

    public HistoriquePenalite toDomain(HistoriquePenaliteEntity e) {
        if (e == null) {
            return null;
        }
        return HistoriquePenalite.builder()
                .idHistoriquePenalite(e.getIdHistoriquePenalite())
                .idCompte(e.getIdCompte())
                .typeLigne(e.getTypeLigne())
                .dateCalcul(e.getDateCalcul())
                .moisRetard(e.getMoisRetard())
                .taux(e.getTaux())
                .base(e.getBase())
                .montant(e.getMontant())
                .createdAt(e.getCreatedAt())
                .build();
    }

    public HistoriquePenaliteEntity toEntity(HistoriquePenalite d) {
        if (d == null) {
            return null;
        }
        return HistoriquePenaliteEntity.builder()
                .idHistoriquePenalite(d.getIdHistoriquePenalite())
                .idCompte(d.getIdCompte())
                .typeLigne(d.getTypeLigne())
                .dateCalcul(d.getDateCalcul())
                .moisRetard(d.getMoisRetard())
                .taux(d.getTaux())
                .base(d.getBase())
                .montant(d.getMontant())
                .createdAt(d.getCreatedAt())
                .build();
    }
}
