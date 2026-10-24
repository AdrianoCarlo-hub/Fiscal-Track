package mg.dgi.fiscaltrack.domain.enums;

/**
 * Situations de majoration IRSA (CDI 20.01.54.1).
 *
 * L'application exacte de chaque situation reste A CONFIRMER (voir
 * QUESTIONS_ENCADREUR.md) : les taux sont fournis, les conditions de
 * declenchement ne le sont pas explicitement.
 */
public enum SituationIrsa {
    /** IRSA non retenu : majoration 40 %. */
    NON_RETENU,
    /** IRSA retenu mais non reverse : majoration 80 %. */
    RETENU_NON_REVERSE,
    /** Retenues intermittentes : 10 % (plancher 20 000 Ar). */
    INTERMITTENT,
    /** Retenues intermittentes retenues : 40 % (plancher 100 000 Ar). */
    INTERMITTENT_RETENU
}
