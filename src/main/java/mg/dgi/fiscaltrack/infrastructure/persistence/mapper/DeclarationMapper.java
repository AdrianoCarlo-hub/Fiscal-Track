package mg.dgi.fiscaltrack.infrastructure.persistence.mapper;

import mg.dgi.fiscaltrack.domain.enums.StatutValidation;
import mg.dgi.fiscaltrack.domain.model.Declaration;
import mg.dgi.fiscaltrack.infrastructure.persistence.entity.DeclarationEntity;
import org.springframework.stereotype.Component;

@Component
public class DeclarationMapper {

    public Declaration toDomain(DeclarationEntity e) {
        if (e == null) {
            return null;
        }
        return Declaration.builder()
                .idDeclaration(e.getIdDeclaration())
                .idObligationFiscale(e.getIdObligationFiscale())
                .chiffreAffairesDeclare(e.getChiffreAffairesDeclare())
                .impotPrincipalCalcule(e.getImpotPrincipalCalcule())
                .dateSoumission(e.getDateSoumission())
                .statutValidation(parseStatutValidation(e.getStatutValidation()))
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }

    public DeclarationEntity toEntity(Declaration d) {
        if (d == null) {
            return null;
        }
        return DeclarationEntity.builder()
                .idDeclaration(d.getIdDeclaration())
                .idObligationFiscale(d.getIdObligationFiscale())
                .chiffreAffairesDeclare(d.getChiffreAffairesDeclare())
                .impotPrincipalCalcule(d.getImpotPrincipalCalcule())
                .dateSoumission(d.getDateSoumission())
                .statutValidation(d.getStatutValidation() == null ? null : d.getStatutValidation().name())
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build();
    }

    private StatutValidation parseStatutValidation(String value) {
        if (value == null) {
            return null;
        }
        try {
            return StatutValidation.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
