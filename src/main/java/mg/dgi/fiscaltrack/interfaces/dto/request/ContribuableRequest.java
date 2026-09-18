package mg.dgi.fiscaltrack.interfaces.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContribuableRequest {

    @NotBlank(message = "Le NIF est obligatoire")
    @Size(min = 10, max = 10, message = "Le NIF doit contenir exactement 10 caracteres")
    private String nif;

    @NotBlank(message = "La raison sociale est obligatoire")
    private String raisonSociale;

    private String formeJuridique;
    private String nomDirigeant;
    private String prenomDirigeant;
    private String cinDirigeant;

    @Email(message = "L'email n'est pas valide")
    private String emailContribuable;

    private String telephoneContribuable;
    private String adresseContribuable;
    private String communeContribuable;

    @NotBlank(message = "Le mot de passe est obligatoire")
    private String motDePasseHashContribuable;

    private String regimeImposition;
    private String obligationComptable;
    private String statutActivite;

    @NotNull(message = "La date d'immatriculation est obligatoire")
    private LocalDate dateImmatriculation;
}
