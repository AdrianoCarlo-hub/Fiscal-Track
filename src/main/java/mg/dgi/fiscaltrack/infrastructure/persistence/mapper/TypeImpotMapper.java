package mg.dgi.fiscaltrack.infrastructure.persistence.mapper;

import mg.dgi.fiscaltrack.domain.enums.Periodicite;
import mg.dgi.fiscaltrack.domain.model.TypeImpot;
import mg.dgi.fiscaltrack.infrastructure.persistence.entity.TypeImpotEntity;
import org.springframework.stereotype.Component;

@Component
public class TypeImpotMapper {

    public TypeImpot toDomain(TypeImpotEntity e) {
        if (e == null) {
            return null;
        }
        return TypeImpot.builder()
                .codeImpot(e.getCodeImpot())
                .nomImpot(e.getNomImpot())
                .periodicite(parsePeriodicite(e.getPeriodicite()))
                .echeanceTheoriqueJour(e.getEcheanceTheoriqueJour())
                .echeanceTheoriqueMois(e.getEcheanceTheoriqueMois())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }

    public TypeImpotEntity toEntity(TypeImpot d) {
        if (d == null) {
            return null;
        }
        return TypeImpotEntity.builder()
                .codeImpot(d.getCodeImpot())
                .nomImpot(d.getNomImpot())
                .periodicite(d.getPeriodicite() == null ? null : d.getPeriodicite().name())
                .echeanceTheoriqueJour(d.getEcheanceTheoriqueJour())
                .echeanceTheoriqueMois(d.getEcheanceTheoriqueMois())
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build();
    }

    private Periodicite parsePeriodicite(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Periodicite.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
