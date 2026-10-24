package mg.dgi.fiscaltrack.infrastructure.persistence.mapper;

import mg.dgi.fiscaltrack.domain.model.ParametreFiscal;
import mg.dgi.fiscaltrack.infrastructure.persistence.entity.ParametreFiscalEntity;
import org.springframework.stereotype.Component;

@Component
public class ParametreFiscalMapper {

    public ParametreFiscal toDomain(ParametreFiscalEntity e) {
        if (e == null) {
            return null;
        }
        return ParametreFiscal.builder()
                .code(e.getCode())
                .valeur(e.getValeur())
                .anneeDebut(e.getAnneeDebut())
                .anneeFin(e.getAnneeFin())
                .baseLegale(e.getBaseLegale())
                .secteur(e.getSecteur())
                .description(e.getDescription())
                .build();
    }

    public ParametreFiscalEntity toEntity(ParametreFiscal d) {
        if (d == null) {
            return null;
        }
        return ParametreFiscalEntity.builder()
                .code(d.getCode())
                .valeur(d.getValeur())
                .anneeDebut(d.getAnneeDebut())
                .anneeFin(d.getAnneeFin())
                .baseLegale(d.getBaseLegale())
                .secteur(d.getSecteur())
                .description(d.getDescription())
                .build();
    }
}
