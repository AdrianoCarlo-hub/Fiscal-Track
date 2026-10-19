package mg.dgi.fiscaltrack.interfaces.controller;

import mg.dgi.fiscaltrack.domain.exception.AccesRefuseException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Lecture de l'utilisateur connecte (aucun passage de parametre NIF depuis le
 * client : le serveur prend toujours son propre contexte d'authentification).
 */
public final class UtilisateurConnecte {

    private UtilisateurConnecte() {
    }

    /** NIF de l'utilisateur connecte s'il s'agit d'un contribuable, sinon null. */
    public static String nifSiContribuable() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return null;
        }
        boolean contribuable = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_CONTRIBUABLE".equals(a.getAuthority()));
        return contribuable ? auth.getName() : null;
    }

    /**
     * Controle de propriete d'une ressource rattachee a un NIF.
     * Les agents (nif = null) passent toujours.
     *
     * @param nifRessource NIF proprietaire de la ressource consultee
     * @param libelle      libelle de l'erreur affichee au client
     */
    public static void verifierPropriete(String nifRessource, String libelle) {
        String nifConnecte = nifSiContribuable();
        if (nifConnecte != null && !nifConnecte.equals(nifRessource)) {
            throw new AccesRefuseException("Acces refuse : " + libelle
                    + " appartient a un autre contribuable.");
        }
    }
}
