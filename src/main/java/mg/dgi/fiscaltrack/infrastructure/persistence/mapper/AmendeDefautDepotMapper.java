package mg.dgi.fiscaltrack.infrastructure.persistence.mapper;

import mg.dgi.fiscaltrack.domain.model.AmendeDefautDepot;
import mg.dgi.fiscaltrack.infrastructure.persistence.entity.AmendeDefautDepotEntity;
import org.springframework.stereotype.Component;

@Component
public class AmendeDefautDepotMapper {

    public AmendeDefautDepot toDomain(AmendeDefautDepotEntity e) {
        if (e == null) {
            return null;
        }
        return AmendeDefautDepot.builder()
                .idAmende(e.getIdAmende())
                .idObligationFiscale(e.getIdObligationFiscale())
                .nif(e.getNif())
                .chiffreAffaires(e.getChiffreAffaires())
                .montant(e.getMontant())
                .dateCalcul(e.getDateCalcul())
                .createdAt(e.getCreatedAt())
                .build();
    }

    public AmendeDefautDepotEntity toEntity(AmendeDefautDepot d) {
        if (d == null) {
            return null;
        }
        return AmendeDefautDepotEntity.builder()
                .idAmende(d.getIdAmende())
                .idObligationFiscale(d.getIdObligationFiscale())
                .nif(d.getNif())
                .chiffreAffaires(d.getChiffreAffaires())
                .montant(d.getMontant())
                .dateCalcul(d.getDateCalcul())
                .createdAt(d.getCreatedAt())
                .build();
    }
}
