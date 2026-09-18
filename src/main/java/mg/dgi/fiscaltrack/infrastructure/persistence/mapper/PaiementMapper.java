package mg.dgi.fiscaltrack.infrastructure.persistence.mapper;

import mg.dgi.fiscaltrack.domain.enums.ModePaiement;
import mg.dgi.fiscaltrack.domain.model.Paiement;
import mg.dgi.fiscaltrack.infrastructure.persistence.entity.PaiementEntity;
import org.springframework.stereotype.Component;

@Component
public class PaiementMapper {

    public Paiement toDomain(PaiementEntity e) {
        if (e == null) {
            return null;
        }
        return Paiement.builder()
                .idPaiement(e.getIdPaiement())
                .idCompte(e.getIdCompte())
                .montantVerse(e.getMontantVerse())
                .datePaiement(e.getDatePaiement())
                .modePaiement(parseModePaiement(e.getModePaiement()))
                .referenceTransaction(e.getReferenceTransaction())
                .createdAt(e.getCreatedAt())
                .build();
    }

    public PaiementEntity toEntity(Paiement d) {
        if (d == null) {
            return null;
        }
        return PaiementEntity.builder()
                .idPaiement(d.getIdPaiement())
                .idCompte(d.getIdCompte())
                .montantVerse(d.getMontantVerse())
                .datePaiement(d.getDatePaiement())
                .modePaiement(d.getModePaiement() == null ? null : d.getModePaiement().name())
                .referenceTransaction(d.getReferenceTransaction())
                .createdAt(d.getCreatedAt())
                .build();
    }

    private ModePaiement parseModePaiement(String value) {
        if (value == null) {
            return null;
        }
        try {
            return ModePaiement.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
