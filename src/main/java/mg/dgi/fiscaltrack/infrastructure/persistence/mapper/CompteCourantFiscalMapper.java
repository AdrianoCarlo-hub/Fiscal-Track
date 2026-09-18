package mg.dgi.fiscaltrack.infrastructure.persistence.mapper;

import mg.dgi.fiscaltrack.domain.enums.StatutRecouvrement;
import mg.dgi.fiscaltrack.domain.model.CompteCourantFiscal;
import mg.dgi.fiscaltrack.infrastructure.persistence.entity.CompteCourantFiscalEntity;
import org.springframework.stereotype.Component;

@Component
public class CompteCourantFiscalMapper {

    public CompteCourantFiscal toDomain(CompteCourantFiscalEntity e) {
        if (e == null) {
            return null;
        }
        return CompteCourantFiscal.builder()
                .idCompte(e.getIdCompte())
                .idDeclaration(e.getIdDeclaration())
                .nif(e.getNif())
                .dateCreationLigne(e.getDateCreationLigne())
                .montantPrincipal(e.getMontantPrincipal())
                .montantPenalites(e.getMontantPenalites())
                .montantPaye(e.getMontantPaye())
                .montantTotalDu(e.getMontantTotalDu())
                .resteARecouvrer(e.getResteARecouvrer())
                .statutRecouvrement(parseStatutRecouvrement(e.getStatutRecouvrement()))
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }

    public CompteCourantFiscalEntity toEntity(CompteCourantFiscal d) {
        if (d == null) {
            return null;
        }
        return CompteCourantFiscalEntity.builder()
                .idCompte(d.getIdCompte())
                .idDeclaration(d.getIdDeclaration())
                .nif(d.getNif())
                .dateCreationLigne(d.getDateCreationLigne())
                .montantPrincipal(d.getMontantPrincipal())
                .montantPenalites(d.getMontantPenalites())
                .montantPaye(d.getMontantPaye())
                .statutRecouvrement(d.getStatutRecouvrement() == null ? null : d.getStatutRecouvrement().name())
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build();
    }

    private StatutRecouvrement parseStatutRecouvrement(String value) {
        if (value == null) {
            return null;
        }
        try {
            return StatutRecouvrement.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
