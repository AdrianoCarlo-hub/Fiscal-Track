package mg.dgi.fiscaltrack.interfaces.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContribuableResponse {

    private String nif;
    private String raisonSociale;
    private String formeJuridique;
    private String nomDirigeant;
    private String prenomDirigeant;
    private String cinDirigeant;
    private String emailContribuable;
    private String telephoneContribuable;
    private String adresseContribuable;
    private String communeContribuable;
    private String regimeImposition;
    private String obligationComptable;
    private String statutActivite;
    private LocalDate dateImmatriculation;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
