package mg.dgi.fiscaltrack.infrastructure.persistence.mapper;

import mg.dgi.fiscaltrack.domain.model.JourFerie;
import mg.dgi.fiscaltrack.infrastructure.persistence.entity.JourFerieEntity;
import org.springframework.stereotype.Component;

@Component
public class JourFerieMapper {

    public JourFerie toDomain(JourFerieEntity e) {
        if (e == null) {
            return null;
        }
        return JourFerie.builder()
                .idJourFerie(e.getIdJourFerie())
                .dateFerie(e.getDateFerie())
                .description(e.getDescription())
                .createdAt(e.getCreatedAt())
                .build();
    }

    public JourFerieEntity toEntity(JourFerie d) {
        if (d == null) {
            return null;
        }
        return JourFerieEntity.builder()
                .idJourFerie(d.getIdJourFerie())
                .dateFerie(d.getDateFerie())
                .description(d.getDescription())
                .createdAt(d.getCreatedAt())
                .build();
    }
}
