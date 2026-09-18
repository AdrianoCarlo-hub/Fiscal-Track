package mg.dgi.fiscaltrack.interfaces.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaiementRequest {

    @NotNull(message = "L'identifiant du compte est obligatoire")
    private Long idCompte;

    @NotNull(message = "Le montant verse est obligatoire")
    @Positive(message = "Le montant verse doit etre strictement positif")
    private BigDecimal montantVerse;

    @NotBlank(message = "Le mode de paiement est obligatoire")
    private String modePaiement;

    private String referenceTransaction;
}
