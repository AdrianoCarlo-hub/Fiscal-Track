package mg.dgi.fiscaltrack.infrastructure.persistence.mapper;

import mg.dgi.fiscaltrack.domain.enums.Division;
import mg.dgi.fiscaltrack.domain.enums.RoleSecurite;
import mg.dgi.fiscaltrack.domain.model.AgentFiscal;
import mg.dgi.fiscaltrack.infrastructure.persistence.entity.AgentFiscalEntity;
import org.springframework.stereotype.Component;

@Component
public class AgentFiscalMapper {

    public AgentFiscal toDomain(AgentFiscalEntity e) {
        if (e == null) {
            return null;
        }
        return AgentFiscal.builder()
                .idAgent(e.getIdAgent())
                .nomAgent(e.getNomAgent())
                .prenomAgent(e.getPrenomAgent())
                .emailAgent(e.getEmailAgent())
                .telephoneAgent(e.getTelephoneAgent())
                .motDePasseHashAgent(e.getMotDePasseHashAgent())
                .division(parseDivision(e.getDivision()))
                .roleSecurite(parseRoleSecurite(e.getRoleSecurite()))
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }

    public AgentFiscalEntity toEntity(AgentFiscal d) {
        if (d == null) {
            return null;
        }
        return AgentFiscalEntity.builder()
                .idAgent(d.getIdAgent())
                .nomAgent(d.getNomAgent())
                .prenomAgent(d.getPrenomAgent())
                .emailAgent(d.getEmailAgent())
                .telephoneAgent(d.getTelephoneAgent())
                .motDePasseHashAgent(d.getMotDePasseHashAgent())
                .division(d.getDivision() == null ? null : d.getDivision().name())
                .roleSecurite(d.getRoleSecurite() == null ? null : d.getRoleSecurite().name())
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build();
    }

    private Division parseDivision(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Division.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private RoleSecurite parseRoleSecurite(String value) {
        if (value == null) {
            return null;
        }
        try {
            return RoleSecurite.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
