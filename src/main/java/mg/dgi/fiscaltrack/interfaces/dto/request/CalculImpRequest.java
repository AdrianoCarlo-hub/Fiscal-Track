package mg.dgi.fiscaltrack.interfaces.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Requete de simulation IMP (impot sur les marches publics, 8 %).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CalculImpRequest {

    @NotNull(message = "Le montant du marche est obligatoire")
    @PositiveOrZero(message = "Le montant du marche doit etre positif ou nul")
    private BigDecimal montantMarche;

    @PositiveOrZero(message = "Les acomptes doivent etre positifs ou nuls")
    private BigDecimal acomptes;
}
