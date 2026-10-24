package mg.dgi.fiscaltrack.application.usecase.notification;

/**
 * Construction d'un message d'erreur court et sans secret, stockable dans
 * la colonne erreur_envoi et affichable dans l'interface.
 */
public final class MessagesErreurEnvoi {

    private static final int LONGUEUR_MAX = 200;

    private MessagesErreurEnvoi() {
    }

    public static String courte(Throwable t) {
        if (t == null) {
            return "Erreur inconnue";
        }
        String cause = t.getMessage();
        if (cause == null || cause.isBlank()) {
            cause = t.getClass().getSimpleName();
        } else {
            cause = t.getClass().getSimpleName() + " : " + cause;
        }
        cause = cause.replaceAll("\\s+", " ").trim();
        if (cause.length() > LONGUEUR_MAX) {
            cause = cause.substring(0, LONGUEUR_MAX) + "…";
        }
        return cause;
    }
}
