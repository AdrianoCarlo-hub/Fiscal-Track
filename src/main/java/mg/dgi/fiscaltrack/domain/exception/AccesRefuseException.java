package mg.dgi.fiscaltrack.domain.exception;

/**
 * Levee quand un utilisateur tente d'acceder a une donnee (obligation,
 * declaration, dossier) qui n'appartient pas a son NIF.
 * Traitee en HTTP 403 (acces refuse), sans deconnexion.
 */
public class AccesRefuseException extends RuntimeException {

    public AccesRefuseException(String message) {
        super(message);
    }
}
