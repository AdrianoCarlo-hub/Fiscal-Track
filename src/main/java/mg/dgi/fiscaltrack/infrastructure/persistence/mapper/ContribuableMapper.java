package mg.dgi.fiscaltrack.infrastructure.persistence.mapper;

import mg.dgi.fiscaltrack.domain.enums.FormeJuridique;
import mg.dgi.fiscaltrack.domain.enums.ObligationComptable;
import mg.dgi.fiscaltrack.domain.enums.RegimeImposition;
import mg.dgi.fiscaltrack.domain.enums.StatutActivite;
import mg.dgi.fiscaltrack.domain.model.Contribuable;
import mg.dgi.fiscaltrack.infrastructure.persistence.entity.ContribuableEntity;
import org.springframework.stereotype.Component;

@Component
public class ContribuableMapper {

    public Contribuable toDomain(ContribuableEntity e) {
        if (e == null) {
            return null;
        }
        return Contribuable.builder()
                .nif(e.getNif())
                .raisonSociale(e.getRaisonSociale())
                .formeJuridique(parseEnum(FormeJuridique.class, e.getFormeJuridique()))
                .nomDirigeant(e.getNomDirigeant())
                .prenomDirigeant(e.getPrenomDirigeant())
                .cinDirigeant(e.getCinDirigeant())
                .emailContribuable(e.getEmailContribuable())
                .telephoneContribuable(e.getTelephoneContribuable())
                .adresseContribuable(e.getAdresseContribuable())
                .communeContribuable(e.getCommuneContribuable())
                .motDePasseHashContribuable(e.getMotDePasseHashContribuable())
                .regimeImposition(parseEnum(RegimeImposition.class, e.getRegimeImposition()))
                .obligationComptable(parseEnum(ObligationComptable.class, e.getObligationComptable()))
                .statutActivite(parseEnum(StatutActivite.class, e.getStatutActivite()))
                .dateImmatriculation(e.getDateImmatriculation())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }

    public ContribuableEntity toEntity(Contribuable d) {
        if (d == null) {
            return null;
        }
        return ContribuableEntity.builder()
                .nif(d.getNif())
                .raisonSociale(d.getRaisonSociale())
                .formeJuridique(enumToString(d.getFormeJuridique()))
                .nomDirigeant(d.getNomDirigeant())
                .prenomDirigeant(d.getPrenomDirigeant())
                .cinDirigeant(d.getCinDirigeant())
                .emailContribuable(d.getEmailContribuable())
                .telephoneContribuable(d.getTelephoneContribuable())
                .adresseContribuable(d.getAdresseContribuable())
                .communeContribuable(d.getCommuneContribuable())
                .motDePasseHashContribuable(d.getMotDePasseHashContribuable())
                .regimeImposition(enumToString(d.getRegimeImposition()))
                .obligationComptable(enumToString(d.getObligationComptable()))
                .statutActivite(enumToString(d.getStatutActivite()))
                .dateImmatriculation(d.getDateImmatriculation())
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build();
    }

    private <E extends Enum<E>> E parseEnum(Class<E> clazz, String value) {
        if (value == null) {
            return null;
        }
        try {
            return Enum.valueOf(clazz, value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String enumToString(Enum<?> e) {
        return e == null ? null : e.name();
    }
}
