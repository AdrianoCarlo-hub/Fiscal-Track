package mg.dgi.fiscaltrack.domain.enums;

/**
 * Niveaux de majoration TVA (CDI 20.01.54.2) : 10 / 40 / 80 / 150 %.
 *
 * La correspondance entre un niveau et un cas d'infraction precis reste
 * A CONFIRMER (voir QUESTIONS_ENCADREUR.md) : seuls les taux sont fournis.
 */
public enum NiveauMajorationTva {
    NIVEAU_1,
    NIVEAU_2,
    NIVEAU_3,
    NIVEAU_4
}
