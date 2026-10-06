package mg.dgi.fiscaltrack.interfaces.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CalculIrRequest {

    @NotNull(message = "Le chiffre d'affaires HT est obligatoire")
    @PositiveOrZero(message = "Le chiffre d'affaires HT doit etre positif ou nul")
    private BigDecimal caHT;

    private BigDecimal chargesDeductibles;
    private BigDecimal reintegrations;
    private BigDecimal deductions;
    private BigDecimal deficitsReportables;

    @NotNull(message = "La categorie d'activite est obligatoire")
    private String categorieActivite;

    private BigDecimal acomptes;

    @PositiveOrZero(message = "Les mois actifs doivent etre positifs")
    private Integer moisActifs;
}
