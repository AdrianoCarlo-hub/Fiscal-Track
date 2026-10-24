package mg.dgi.fiscaltrack.interfaces.dto.request;

import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Preferences de contact d'un contribuable (canal prefere et consentements).
 * Chaque champ null conserve la valeur existante.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PreferencesContactRequest {

    /** EMAIL, SMS ou PORTAIL. Vide = tous les canaux. */
    @Pattern(regexp = "^$|^(EMAIL|SMS|PORTAIL)$",
            message = "Le canal préféré doit être EMAIL, SMS ou PORTAIL")
    private String canalPrefere;

    private Boolean accepteEmail;

    private Boolean accepteSms;
}
