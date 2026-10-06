package mg.dgi.fiscaltrack.interfaces.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CalculIsRequest {

    @NotNull(message = "Le chiffre d'affaires brut est obligatoire")
    @PositiveOrZero(message = "Le chiffre d'affaires brut doit etre positif ou nul")
    private BigDecimal caBrut;

    private Boolean adherentCga;
    private BigDecimal achatsConformes;
    private BigDecimal chargesSalariales;
    private BigDecimal acomptes;
}
