package mg.dgi.fiscaltrack.interfaces.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Requete de simulation IRCM (impot sur les revenus des capitaux mobiliers).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CalculIrcmRequest {

    @NotNull(message = "Le chiffre d'affaires brut est obligatoire")
    @PositiveOrZero(message = "Le chiffre d'affaires doit etre positif ou nul")
    private BigDecimal caBrut;

    private Boolean estCooperative;

    @PositiveOrZero(message = "Les acomptes doivent etre positifs ou nuls")
    private BigDecimal acomptes;
}
