package mg.dgi.fiscaltrack.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mg.dgi.fiscaltrack.domain.enums.FormeJuridique;
import mg.dgi.fiscaltrack.domain.enums.ObligationComptable;
import mg.dgi.fiscaltrack.domain.enums.RegimeImposition;
import mg.dgi.fiscaltrack.domain.enums.StatutActivite;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Contribuable {

    private String nif;
    private String raisonSociale;
    private FormeJuridique formeJuridique;
    private String nomDirigeant;
    private String prenomDirigeant;
    private String cinDirigeant;
    private String emailContribuable;
    private String telephoneContribuable;
    private String adresseContribuable;
    private String communeContribuable;
    private String motDePasseHashContribuable;
    private RegimeImposition regimeImposition;
    private ObligationComptable obligationComptable;
    private StatutActivite statutActivite;
    private LocalDate dateImmatriculation;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
