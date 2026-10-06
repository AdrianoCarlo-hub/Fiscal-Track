package mg.dgi.fiscaltrack.application.usecase.calculfiscal;

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
 * Regles :
 *   - Taux : 5%
 *   - CGA : abattement 50% plafonne a 5 000 000 Ar
 *   - Reductions : -2% achats conformes, -2% charges salariales
 *   - Plancher : IS apres reduction >= 3% du CA brut
 */
@Service
public class CalculerIsUseCase {

    private static final BigDecimal TAUX_IS = new BigDecimal("0.05");
    private static final BigDecimal TAUX_ABATTEMENT_CGA = new BigDecimal("0.50");
    private static final BigDecimal PLAFOND_ABATTEMENT_CGA = new BigDecimal("5000000");
    private static final BigDecimal TAUX_REDUCTION = new BigDecimal("0.02");
    private static final BigDecimal TAUX_PLANCHER = new BigDecimal("0.03");

    public ResultatCalculFiscal execute(BigDecimal caBrut,
                                          boolean adherentCga,
                                          BigDecimal achatsConformes,
                                          BigDecimal chargesSalariales,
                                          BigDecimal acomptes) {

        if (caBrut == null || caBrut.signum() < 0) {
            throw new CalculFiscalInvalideException("Le chiffre d'affaires brut est obligatoire et positif");
        }

        // Arrondi a la dizaine de milliers inferieure
        BigDecimal base = arrondirDizaineMilliers(caBrut);

        // Abattement CGA
        BigDecimal abattement = BigDecimal.ZERO;
        if (adherentCga) {
            abattement = base.multiply(TAUX_ABATTEMENT_CGA).min(PLAFOND_ABATTEMENT_CGA);
            base = base.subtract(abattement);
        }

        // IS theorique
        BigDecimal isTheorique = base.multiply(TAUX_IS).setScale(2, RoundingMode.HALF_UP);

        // Reductions
        BigDecimal reductionAchats = nvl(achatsConformes).multiply(TAUX_REDUCTION);
        BigDecimal reductionSalaires = nvl(chargesSalariales).multiply(TAUX_REDUCTION);
        BigDecimal reductionsTotal = reductionAchats.add(reductionSalaires);

        BigDecimal isApresReduction = isTheorique.subtract(reductionsTotal);
        if (isApresReduction.signum() < 0) isApresReduction = BigDecimal.ZERO;

        // Plancher : IS >= 3% du CA brut
        BigDecimal plancher = caBrut.multiply(TAUX_PLANCHER).setScale(2, RoundingMode.HALF_UP);
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
        etapes.add("IS theorique (5%) : " + isTheorique + " Ar");
        etapes.add("Reductions : -" + reductionsTotal + " Ar");
        etapes.add("Plancher (3% du CA) : " + plancher + " Ar");
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
                .tauxApplique(TAUX_IS)
                .montantBrut(isTheorique)
                .montantMinimum(plancher)
                .reductions(reductionsTotal)
                .acomptesDeduits(acomptesNvl)
                .creditFiscal(credit)
                .solde(solde)
                .explication("IS = MAX(CA x 5% - reductions ; 3% du CA) - acomptes")
                .etapesCalcul(etapes)
                .build();
    }

    private BigDecimal arrondirDizaineMilliers(BigDecimal valeur) {
        return valeur.divide(BigDecimal.valueOf(10000), 0, RoundingMode.FLOOR)
                .multiply(BigDecimal.valueOf(10000));
    }

    private BigDecimal nvl(BigDecimal valeur) {
        return valeur == null ? BigDecimal.ZERO : valeur;
    }
}
