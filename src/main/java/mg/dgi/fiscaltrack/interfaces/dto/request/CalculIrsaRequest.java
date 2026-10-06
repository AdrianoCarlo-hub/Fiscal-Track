package mg.dgi.fiscaltrack.interfaces.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CalculIrsaRequest {

    @NotNull(message = "Le salaire brut est obligatoire")
    @PositiveOrZero(message = "Le salaire brut doit etre positif ou nul")
    private BigDecimal salaireBrut;

    private BigDecimal cotisationsSociales;
    private BigDecimal avantageLogement;
    private BigDecimal avantageVehicule;
    private BigDecimal avantageTelephone;

    @PositiveOrZero(message = "Le nombre de personnes a charge doit etre positif")
    private Integer nombrePersonnesCharge;
}
