package mg.dgi.fiscaltrack.interfaces.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Requete de simulation IPVI (impot sur les plus-values immobilieres, 20 %).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CalculIpviRequest {

    @NotNull(message = "Le prix de vente est obligatoire")
    @PositiveOrZero(message = "Le prix de vente doit etre positif ou nul")
    private BigDecimal prixVente;

    @PositiveOrZero(message = "Les acomptes doivent etre positifs ou nuls")
    private BigDecimal acomptes;
}
