package mg.dgi.fiscaltrack.interfaces.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TypeImpotRequest {

    @NotBlank(message = "Le code impot est obligatoire")
    private String codeImpot;

    @NotBlank(message = "Le nom de l'impot est obligatoire")
    private String nomImpot;

    @NotBlank(message = "La periodicite est obligatoire")
    private String periodicite;

    @NotNull(message = "Le jour d'echeance est obligatoire")
    @Min(value = 1, message = "Le jour doit etre entre 1 et 31")
    @Max(value = 31, message = "Le jour doit etre entre 1 et 31")
    private Integer echeanceTheoriqueJour;

    @Min(value = 1, message = "Le mois doit etre entre 1 et 12")
    @Max(value = 12, message = "Le mois doit etre entre 1 et 12")
    private Integer echeanceTheoriqueMois;
}
