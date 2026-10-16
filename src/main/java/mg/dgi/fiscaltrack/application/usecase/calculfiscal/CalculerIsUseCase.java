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
 *   - Minimum selon la categorie d'activite (16 000 / 50 000 / 100 000 / 150 000)
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
        return execute(caBrut, null, adherentCga, achatsConformes, chargesSalariales, acomptes);
    }

    public ResultatCalculFiscal execute(BigDecimal caBrut,
                                          String categorieActivite,
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

        // Minimum selon la categorie d'activite (16 000 / 50 000 / 100 000 / 150 000)
        BigDecimal minimumCategorie = calculerMinimumCategorie(categorieActivite);
        BigDecimal plancherEffectif = plancher.max(minimumCategorie);
        BigDecimal isDu = isApresReduction.max(plancherEffectif);

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
        if (minimumCategorie.signum() > 0) {
            etapes.add("Minimum de la categorie : " + minimumCategorie + " Ar");
        }
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
                .montantMinimum(plancherEffectif)
                .reductions(reductionsTotal)
                .acomptesDeduits(acomptesNvl)
                .creditFiscal(credit)
                .solde(solde)
                .explication("IS = MAX(CA x " + (tauxIs.multiply(new BigDecimal(100))) + "% - reductions ; " + (tauxPlancher.multiply(new BigDecimal(100))) + "% du CA) - acomptes")
                .etapesCalcul(etapes)
                .build();
    }

    /**
     * Minimum d'imposition IS selon la categorie d'activite (CDI 01.02.05).
     *   - AGRICOLE, ELEVEUR : 16 000 Ar
     *   - ARTISAN, GARGOTIER, PETIT_COMMERCANT : 50 000 Ar
     *   - COMMERCANT, HOTELIER, PRESTATAIRE : 100 000 Ar
     *   - PROFESSION_LIBERALE, ACTIVITES_MULTIPLES : 150 000 Ar
     * Si la categorie n'est pas renseignee, aucun minimum de categorie n'est
     * applique (seul le plancher de 3% du CA s'applique).
     */
    private BigDecimal calculerMinimumCategorie(String categorie) {
        if (categorie == null || categorie.isBlank()) {
            return BigDecimal.ZERO;
        }
        String cat = categorie.toUpperCase();
        if (cat.equals("AGRICOLE") || cat.equals("ELEVEUR")) {
            return parametreFiscalService.getValeur("IS_MINIMUM_AGRICOLE_ELEVEUR");
        }
        if (cat.equals("ARTISAN") || cat.equals("GARGOTIER") || cat.equals("PETIT_COMMERCANT")) {
            return parametreFiscalService.getValeur("IS_MINIMUM_ARTISAN");
        }
        if (cat.equals("COMMERCANT") || cat.equals("HOTELIER") || cat.equals("PRESTATAIRE")) {
            return parametreFiscalService.getValeur("IS_MINIMUM_COMMERCANT");
        }
        if (cat.equals("PROFESSION_LIBERALE") || cat.equals("ACTIVITES_MULTIPLES")) {
            return parametreFiscalService.getValeur("IS_MINIMUM_PROFESSION_LIBERALE");
        }
        return BigDecimal.ZERO;
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
