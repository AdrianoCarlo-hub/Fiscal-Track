package mg.dgi.fiscaltrack.interfaces.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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

    /** Format accepte : +261XXXXXXXXX ou 03XXXXXXXX (10 chiffres). */
    @Pattern(regexp = "^$|^\\+261\\d{9}$|^03\\d{8}$",
            message = "Le telephone doit être au format +261XXXXXXXXX ou 03XXXXXXXX")
    private String telephoneContribuable;
    private String adresseContribuable;
    private String communeContribuable;

    /**
     * Mot de passe en clair fourni par le client. Il est encode (BCrypt) cote
     * serveur avant persistance et n'est jamais renvoye dans les reponses.
     * Obligatoire a la creation, facultatif a la modification (si absent, le
     * mot de passe existant est conserve).
     */
    private String motDePasseHashContribuable;

    private String regimeImposition;
    private String obligationComptable;
    private String statutActivite;

    @NotNull(message = "La date d'immatriculation est obligatoire")
    private LocalDate dateImmatriculation;

    private Boolean autorisationOuverture;

    private Boolean optionSante;

    /** Date de cloture de l'exercice comptable (ex. 30 juin). Null = exercice civil. */
    private LocalDate dateCloture;

    /** Categorie d'activite du dossier (pre-remplissage des formulaires IR). */
    private String categorieActivite;

    /** Canal de contact prefere : EMAIL, SMS ou PORTAIL (null = tous). */
    private String canalPrefere;

    /** Le contribuable accepte les e-mails (null = conserve l'existant). */
    private Boolean accepteEmail;

    /** Le contribuable accepte les SMS (null = conserve l'existant). */
    private Boolean accepteSms;
}
