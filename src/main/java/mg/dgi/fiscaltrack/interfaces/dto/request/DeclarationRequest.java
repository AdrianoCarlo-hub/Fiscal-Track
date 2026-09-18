package mg.dgi.fiscaltrack.interfaces.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeclarationRequest {

    @NotNull(message = "L'identifiant de l'obligation est obligatoire")
    private Long idObligationFiscale;

    @NotNull(message = "Le chiffre d'affaires declare est obligatoire")
    @PositiveOrZero(message = "Le chiffre d'affaires ne peut pas etre negatif")
    private BigDecimal chiffreAffairesDeclare;

    @NotNull(message = "L'impot principal calcule est obligatoire")
    @PositiveOrZero(message = "L'impot principal ne peut pas etre negatif")
    private BigDecimal impotPrincipalCalcule;
}
