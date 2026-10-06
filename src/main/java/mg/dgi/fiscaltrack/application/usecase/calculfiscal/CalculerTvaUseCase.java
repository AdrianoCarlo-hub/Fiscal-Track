package mg.dgi.fiscaltrack.application.usecase.calculfiscal;

import mg.dgi.fiscaltrack.domain.exception.CalculFiscalInvalideException;
import mg.dgi.fiscaltrack.domain.model.ResultatCalculFiscal;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Calcul de la Taxe sur la Valeur Ajoutee (TVA).
 *
 * Taux : 20% (0% pour exportations).
 * TVA nette = TVA collectee - TVA deductible.
 * Si negatif : credit de TVA reportable.
 */
@Service
public class CalculerTvaUseCase {

    private static final BigDecimal TAUX_TVA = new BigDecimal("0.20");

    public ResultatCalculFiscal execute(BigDecimal tvaCollectee,
                                          BigDecimal tvaDeductible,
                                          BigDecimal creditTvaAnterieur,
                                          boolean exportation) {

        if (tvaCollectee == null || tvaCollectee.signum() < 0) {
            throw new CalculFiscalInvalideException("La TVA collectee est obligatoire et positive");
        }
        if (tvaDeductible == null || tvaDeductible.signum() < 0) {
            throw new CalculFiscalInvalideException("La TVA deductibile est obligatoire et positive");
        }

        // TVA nette
        BigDecimal tvaNette = tvaCollectee.subtract(tvaDeductible);

        // Prise en compte du credit anterieur
        BigDecimal creditNvl = nvl(creditTvaAnterieur);
        tvaNette = tvaNette.subtract(creditNvl);

        BigDecimal tvaDue;
        BigDecimal credit = BigDecimal.ZERO;

        if (tvaNette.signum() < 0) {
            credit = tvaNette.abs();
            tvaDue = BigDecimal.ZERO;
        } else {
            tvaDue = tvaNette.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal tauxApplique = exportation ? BigDecimal.ZERO : TAUX_TVA;

        List<String> etapes = new ArrayList<>();
        etapes.add("TVA collectee : " + tvaCollectee + " Ar");
        etapes.add("TVA deductibile : " + tvaDeductible + " Ar");
        if (creditNvl.signum() > 0) {
            etapes.add("Credit TVA anterieur : -" + creditNvl + " Ar");
        }
        etapes.add("TVA nette : " + tvaNette + " Ar");
        if (credit.signum() > 0) {
            etapes.add("Credit TVA a reporter : " + credit + " Ar");
        } else {
            etapes.add("TVA a payer : " + tvaDue + " Ar");
        }

        return ResultatCalculFiscal.builder()
                .typeImpot("TVA")
                .tauxApplique(tauxApplique)
                .baseImposable(tvaCollectee)
                .montantBrut(tvaNette)
                .creditFiscal(credit)
                .solde(tvaDue)
                .explication("TVA = TVA collectee - TVA deductibile - credit anterieur")
                .etapesCalcul(etapes)
                .build();
    }

    private BigDecimal nvl(BigDecimal valeur) {
        return valeur == null ? BigDecimal.ZERO : valeur;
    }
}
