package mg.dgi.fiscaltrack.application.usecase.majoration;

import mg.dgi.fiscaltrack.domain.model.CompteCourantFiscal;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Support commun aux majorations : ajoute le montant aux penalites du compte
 * (donc au total du) et met a jour la date de modification. Ne persiste pas.
 */
final class MajorationSupport {

    private MajorationSupport() {
    }

    static void appliquer(CompteCourantFiscal compte, BigDecimal montant) {
        BigDecimal penalites = compte.getMontantPenalites() == null
                ? BigDecimal.ZERO
                : compte.getMontantPenalites();
        compte.setMontantPenalites(penalites.add(montant));
        compte.setUpdatedAt(OffsetDateTime.now());
    }
}
