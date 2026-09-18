package mg.dgi.fiscaltrack.infrastructure.persistence.mapper;

import mg.dgi.fiscaltrack.domain.enums.StatutDeclaration;
import mg.dgi.fiscaltrack.domain.model.ObligationFiscale;
import mg.dgi.fiscaltrack.infrastructure.persistence.entity.ObligationFiscaleEntity;
import org.springframework.stereotype.Component;

@Component
public class ObligationFiscaleMapper {

    public ObligationFiscale toDomain(ObligationFiscaleEntity e) {
        if (e == null) {
            return null;
        }
        return ObligationFiscale.builder()
                .idObligationFiscale(e.getIdObligationFiscale())
                .nif(e.getNif())
                .codeImpot(e.getCodeImpot())
                .idAgent(e.getIdAgent())
                .idJourFerie(e.getIdJourFerie())
                .periodeFiscale(e.getPeriodeFiscale())
                .dateLimiteTheorique(e.getDateLimiteTheorique())
                .dateLimiteReelle(e.getDateLimiteReelle())
                .statutDeclaration(parseStatutDeclaration(e.getStatutDeclaration()))
                .dateDepotEffectif(e.getDateDepotEffectif())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }

    public ObligationFiscaleEntity toEntity(ObligationFiscale d) {
        if (d == null) {
            return null;
        }
        return ObligationFiscaleEntity.builder()
                .idObligationFiscale(d.getIdObligationFiscale())
                .nif(d.getNif())
                .codeImpot(d.getCodeImpot())
                .idAgent(d.getIdAgent())
                .idJourFerie(d.getIdJourFerie())
                .periodeFiscale(d.getPeriodeFiscale())
                .dateLimiteTheorique(d.getDateLimiteTheorique())
                .dateLimiteReelle(d.getDateLimiteReelle())
                .statutDeclaration(d.getStatutDeclaration() == null ? null : d.getStatutDeclaration().name())
                .dateDepotEffectif(d.getDateDepotEffectif())
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build();
    }

    private StatutDeclaration parseStatutDeclaration(String value) {
        if (value == null) {
            return null;
        }
        try {
            return StatutDeclaration.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
