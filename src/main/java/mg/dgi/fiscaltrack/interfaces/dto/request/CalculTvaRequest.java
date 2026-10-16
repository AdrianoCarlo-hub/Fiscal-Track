package mg.dgi.fiscaltrack.interfaces.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CalculTvaRequest {

    @NotNull(message = "La TVA collectee est obligatoire")
    @PositiveOrZero(message = "La TVA collectee doit etre positive ou nulle")
    private BigDecimal tvaCollectee;

    @NotNull(message = "La TVA deductibile est obligatoire")
    @PositiveOrZero(message = "La TVA deductibile doit etre positive ou nulle")
    private BigDecimal tvaDeductible;

    private BigDecimal creditTvaAnterieur;
    private Boolean exportation;

    /** Application du taux reduit de 5% (gaz butane, CDI 06.01.12). */
    private Boolean gazButane;

    /** Chiffre d'affaires de reference, pour controler le seuil d'assujettissement (400M). */
    private BigDecimal chiffreAffaires;
}
