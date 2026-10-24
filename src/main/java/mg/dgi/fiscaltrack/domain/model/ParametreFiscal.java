package mg.dgi.fiscaltrack.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Year;

/**
 * Parametre fiscal parametrable.
 * Base legale : articles CDI 2024, CPF, IR 2026, IS 2026.
 * Toutes les valeurs fiscales (taux, seuils, minima, plafonds) doivent etre stockes ici.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParametreFiscal {

    private String code;
    private BigDecimal valeur;
    private Year anneeDebut;
    private Year anneeFin;
    private String baseLegale;
    private String secteur;
    private String description;
}
