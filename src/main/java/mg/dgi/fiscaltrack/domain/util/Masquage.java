package mg.dgi.fiscaltrack.domain.util;

/**
 * Utilitaires de masquage pour les journaux et les erreurs stockees.
 * Aucune donnee sensible (NIF complet, e-mail, telephone) n'est ecrite
 * dans les journaux applicatifs.
 */
public final class Masquage {

    private Masquage() {
    }

    /** NIF masque pour les journaux : NIF00…01. */
    public static String masquerNif(String nif) {
        if (nif == null || nif.length() < 4) {
            return "NIF***";
        }
        return nif.substring(0, 3) + "…" + nif.substring(nif.length() - 2);
    }
}
