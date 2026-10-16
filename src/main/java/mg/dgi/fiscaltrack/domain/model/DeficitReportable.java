package mg.dgi.fiscaltrack.domain.model;

import java.math.BigDecimal;

/**
 * Deficit fiscal reportable, rattache a son exercice d'origine.
 *
 * Le report est limite a 5 exercices (CDI 01.01.10 11°) : un deficit ne de
 * l'exercice Y n'est imputable que sur les exercices Y+1 a Y+5.
 *
 * @param anneeOrigine exercice de naissance du deficit
 * @param montant      montant du deficit (positif)
 */
public record DeficitReportable(int anneeOrigine, BigDecimal montant) {
}
