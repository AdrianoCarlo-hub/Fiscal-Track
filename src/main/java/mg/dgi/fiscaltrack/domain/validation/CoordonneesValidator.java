package mg.dgi.fiscaltrack.domain.validation;

import java.util.regex.Pattern;

/**
 * Validation des coordonnees de contact d'un contribuable.
 * Aucune dependance framework : reutilisable depuis les use cases et les tests.
 */
public final class CoordonneesValidator {

    /** +261 suivi de 9 chiffres, ou 03x suivi de 8 chiffres (10 chiffres au total). */
    private static final Pattern TELEPHONE_MALGACHE =
            Pattern.compile("^(?:\\+261\\d{9}|03\\d{8})$");

    private static final Pattern EMAIL =
            Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private CoordonneesValidator() {
    }

    public static boolean estTelephoneValide(String telephone) {
        return telephone != null && TELEPHONE_MALGACHE.matcher(telephone.trim()).matches();
    }

    public static boolean estEmailValide(String email) {
        return email != null && EMAIL.matcher(email.trim()).matches();
    }

    /**
     * Valide les coordonnees fournies (null = non renseigne, autorise).
     *
     * @throws IllegalArgumentException avec un message lisible en francais
     */
    public static void valider(String email, String telephone) {
        if (email != null && !email.isBlank() && !estEmailValide(email)) {
            throw new IllegalArgumentException(
                    "L'adresse e-mail n'est pas valide (format attendu : nom@domaine.mg)");
        }
        if (telephone != null && !telephone.isBlank() && !estTelephoneValide(telephone)) {
            throw new IllegalArgumentException(
                    "Le numero de telephone n'est pas valide (formats acceptes : +261XXXXXXXXX ou 03XXXXXXXX)");
        }
    }
}
