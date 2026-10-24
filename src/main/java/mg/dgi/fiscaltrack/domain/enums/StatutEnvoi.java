package mg.dgi.fiscaltrack.domain.enums;

/**
 * Statut d'une notification.
 *
 * EN_ATTENTE      : creee, pas encore remise au canal.
 * ENVOYE          : remise avec succes au canal (ou publiee sur le portail).
 * ECHEC           : tentative avortee ; une nouvelle tentative est prevue.
 * ECHEC_DEFINITIF : nombre maximum de tentatives atteint ; renvoi manuel
 *                   possible.
 * IGNORE_PREFERENCE : le contribuable refuse ce canal ; aucun envoi effectue.
 */
public enum StatutEnvoi {
    EN_ATTENTE,
    ENVOYE,
    ECHEC,
    ECHEC_DEFINITIF,
    IGNORE_PREFERENCE
}
