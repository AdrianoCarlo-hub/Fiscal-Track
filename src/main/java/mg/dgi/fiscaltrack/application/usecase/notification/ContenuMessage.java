package mg.dgi.fiscaltrack.application.usecase.notification;

/**
 * Textes prets a l'envoi pour une notification.
 */
public record ContenuMessage(String sujet, String corpsEmail, String corpsSms) {
}
