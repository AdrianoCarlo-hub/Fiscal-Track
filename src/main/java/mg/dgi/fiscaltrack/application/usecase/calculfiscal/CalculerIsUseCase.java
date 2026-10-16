package mg.dgi.fiscaltrack.application.usecase.calculfiscal;

import mg.dgi.fiscaltrack.application.usecase.admin.ParametreFiscalService;
import mg.dgi.fiscaltrack.domain.exception.CalculFiscalInvalideException;
import mg.dgi.fiscaltrack.domain.model.ResultatCalculFiscal;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Calcul de l'Impot Synthetique (IS).
 *
 * Regles (CDI 01.02.05, 01.02.05 bis) :
 *   - Taux : 5% (CDI 01.02.05)
 *   - CGA : abattement 50% plafonne a 5 000 000 Ar (CDI 01.02.05 bis)
 *   - Reductions : -2% achats conformes, -2% charges salariales (CDI 01.02.05 bis)
 *   - Plancher : IS apres reduction >= 3% du CA brut (CDI 01.02.05)
 *   - Arrondi a la dizaine de milliers inferieure
 */
@Service
public class CalculerIsUseCase {

    private final ParametreFiscalService parametreFiscalService;

    public CalculerIsUseCase(ParametreFiscalService parametreFiscalService) {
        this.parametreFiscalService = parametreFiscalService;
    }

    public ResultatCalculFiscal execute(BigDecimal caBrut,
                                          boolean adherentCga,
                                          BigDecimal achatsConformes,
                                          BigDecimal chargesSalariales,
                                          BigDecimal acomptes) {

        if (caBrut == null || caBrut.signum() < 0) {
            throw new CalculFiscalInvalideException("Le chiffre d'affaires brut est obligatoire et positif");
        }

        // Arrondi a la dizaine de milliers inferieure (CDI 01.02.05)
        BigDecimal base = arrondirDizaineMilliers(caBrut);

        // Abattement CGA (CDI 01.02.05 bis)
        BigDecimal abattement = BigDecimal.ZERO;
        if (adherentCga) {
            BigDecimal tauxAbattement = parametreFiscalService.getValeur("IS_TAUX_ABATTEMENT_CGA");
            BigDecimal plafondAbattement = parametreFiscalService.getValeur("IS_PLAFOND_ABATTEMENT_CGA");
            abattement = base.multiply(tauxAbattement).min(plafondAbattement);
            base = base.subtract(abattement);
        }

        // IS theorique (CDI 01.02.05)
        BigDecimal tauxIs = parametreFiscalService.getValeur("IS_TAUX");
        BigDecimal isTheorique = base.multiply(tauxIs).setScale(2, RoundingMode.HALF_UP);

        // Reductions (CDI 01.02.05 bis)
        BigDecimal tauxReduction = parametreFiscalService.getValeur("IS_TAUX_REDUCTION");
        BigDecimal reductionAchats = nvl(achatsConformes).multiply(tauxReduction);
        BigDecimal reductionSalaires = nvl(chargesSalariales).multiply(tauxReduction);
        BigDecimal reductionsTotal = reductionAchats.add(reductionSalaires);

        BigDecimal isApresReduction = isTheorique.subtract(reductionsTotal);
        if (isApresReduction.signum() < 0) isApresReduction = BigDecimal.ZERO;

        // Plancher : IS >= 3% du CA brut (CDI 01.02.05)
        BigDecimal tauxPlancher = parametreFiscalService.getValeur("IS_TAUX_PLANCHER");
        BigDecimal plancher = caBrut.multiply(tauxPlancher).setScale(2, RoundingMode.HALF_UP);
        BigDecimal isDu = isApresReduction.max(plancher);

        // Acomptes
        BigDecimal acomptesNvl = nvl(acomptes);
        BigDecimal solde = isDu.subtract(acomptesNvl);

        BigDecimal credit = BigDecimal.ZERO;
        if (solde.signum() < 0) {
            credit = solde.abs();
            solde = BigDecimal.ZERO;
        }

        List<String> etapes = new ArrayList<>();
        etapes.add("CA brut : " + caBrut + " Ar");
        etapes.add("Base apres arrondi : " + base + " Ar");
        if (adherentCga) {
            etapes.add("Abattement CGA : -" + abattement + " Ar");
        }
        etapes.add("IS theorique (" + (tauxIs.multiply(new BigDecimal(100))) + "%) : " + isTheorique + " Ar");
        etapes.add("Reductions : -" + reductionsTotal + " Ar");
        etapes.add("Plancher (" + (tauxPlancher.multiply(new BigDecimal(100))) + "% du CA) : " + plancher + " Ar");
        etapes.add("IS retenu : " + isDu + " Ar");
        etapes.add("Acomptes : " + acomptesNvl + " Ar");
        if (credit.signum() > 0) {
            etapes.add("Credit : " + credit + " Ar");
        } else {
            etapes.add("Solde a payer : " + solde + " Ar");
        }

        return ResultatCalculFiscal.builder()
                .typeImpot("IS")
                .baseImposable(base)
                .tauxApplique(tauxIs)
                .montantBrut(isTheorique)
                .montantMinimum(plancher)
                .reductions(reductionsTotal)
                .acomptesDeduits(acomptesNvl)
                .creditFiscal(credit)
                .solde(solde)
                .explication("IS = MAX(CA x " + (tauxIs.multiply(new BigDecimal(100))) + "% - reductions ; " + (tauxPlancher.multiply(new BigDecimal(100))) + "% du CA) - acomptes")
                .etapesCalcul(etapes)
                .build();
    }

    /**
     * Arrondi a la dizaine de milliers inferieure (CDI 01.02.05).
     */
    private BigDecimal arrondirDizaineMilliers(BigDecimal valeur) {
        return valeur.divide(BigDecimal.valueOf(10000), 0, RoundingMode.FLOOR)
                .multiply(BigDecimal.valueOf(10000));
    }

    private BigDecimal nvl(BigDecimal valeur) {
        return valeur == null ? BigDecimal.ZERO : valeur;
    }
}
