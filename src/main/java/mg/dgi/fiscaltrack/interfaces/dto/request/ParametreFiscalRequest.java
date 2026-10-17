package mg.dgi.fiscaltrack.interfaces.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Requete de modification d'un parametre fiscal (page ADMIN).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParametreFiscalRequest {

    @NotNull(message = "La valeur est obligatoire")
    private BigDecimal valeur;

    private Integer anneeDebut;
    private Integer anneeFin;
    private String baseLegale;
    private String secteur;
    private String description;
}
