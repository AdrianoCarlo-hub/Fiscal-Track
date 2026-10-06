package mg.dgi.fiscaltrack.application.usecase.calculfiscal;

import mg.dgi.fiscaltrack.domain.enums.CategorieActivite;
import mg.dgi.fiscaltrack.domain.exception.CalculFiscalInvalideException;
import mg.dgi.fiscaltrack.domain.model.ResultatCalculFiscal;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Calcul de l'Impot sur les Revenus (IR).
 *
 * Regles :
 *   - Taux normal : 20%
 *   - Taux reduit education/sante : 10%
 *   - Minimum selon categorie d'activite
 *   - Deficits reportables max 5 ans (a verifier par l'appelant)
 *   - Arrondi au millier inferieur
 */
@Service
public class CalculerIrUseCase {

    private static final BigDecimal TAUX_NORMAL = new BigDecimal("0.20");
    private static final BigDecimal TAUX_REDUIT = new BigDecimal("0.10");
    private static final BigDecimal TAUX_CARBURANT = new BigDecimal("0.002");

    private static final BigDecimal PART_FIXE_AGRICOLE = new BigDecimal("500000");
    private static final BigDecimal PART_FIXE_AUTRES = new BigDecimal("1000000");
    private static final BigDecimal PART_FIXE_EDUCATION_SANTE = new BigDecimal("200000");
    private static final BigDecimal MINIMUM_ASSOCIE_SARL = new BigDecimal("320000");
    private static final BigDecimal ABATTEMENT_ASSOCIE_SARL = new BigDecimal("4200000");

    public ResultatCalculFiscal execute(BigDecimal caHT,
                                          BigDecimal chargesDeductibles,
                                          BigDecimal reintegrations,
                                          BigDecimal deductions,
                                          BigDecimal deficitsReportables,
                                          CategorieActivite categorie,
                                          BigDecimal acomptes,
                                          int moisActifs) {

        // Validations
        if (caHT == null || caHT.signum() < 0) {
            throw new CalculFiscalInvalideException("Le chiffre d'affaires HT est obligatoire et positif");
        }
        if (moisActifs < 0 || moisActifs > 12) {
            throw new CalculFiscalInvalideException("Les mois actifs doivent etre entre 0 et 12");
        }
        if (categorie == null) {
            throw new CalculFiscalInvalideException("La categorie d'activite est obligatoire");
        }

        // Calcul de la base imposable
        BigDecimal base = caHT
                .subtract(nvl(chargesDeductibles))
                .add(nvl(reintegrations))
                .subtract(nvl(deductions))
                .subtract(nvl(deficitsReportables));

        // Abattement special pour associe-gerant SARL
        if (categorie == CategorieActivite.ASSOCIE_GERANT_SARL) {
            base = base.subtract(ABATTEMENT_ASSOCIE_SARL);
            if (base.signum() < 0) base = BigDecimal.ZERO;
        }

        // Arrondi au millier inferieur
        base = arrondirMillier(base);

        // Determination du taux
        BigDecimal taux = determinerTaux(categorie);

        // IR theorique
        BigDecimal irTheorique = base.multiply(taux).setScale(2, RoundingMode.HALF_UP);

        // Minimum de perception
        BigDecimal minimum = calculerMinimum(caHT, categorie, moisActifs);

        // IR du = MAX(irTheorique, minimum)
        BigDecimal irDu = irTheorique.max(minimum);

        // Acomptes
        BigDecimal acomptesNvl = nvl(acomptes);

        // Solde
        BigDecimal solde = irDu.subtract(acomptesNvl);

        // Credit
        BigDecimal credit = BigDecimal.ZERO;
        if (solde.signum() < 0) {
            credit = solde.abs();
            solde = BigDecimal.ZERO;
        }

        // Etapes de calcul
        List<String> etapes = new ArrayList<>();
        etapes.add("Base imposable : " + base + " Ar");
        etapes.add("Taux applique : " + taux.multiply(new BigDecimal("100")) + " %");
        etapes.add("IR theorique : " + irTheorique + " Ar");
        etapes.add("Minimum de perception : " + minimum + " Ar");
        etapes.add("IR retenu : MAX(" + irTheorique + " ; " + minimum + ") = " + irDu + " Ar");
        etapes.add("Acomptes deduits : " + acomptesNvl + " Ar");
        if (credit.signum() > 0) {
            etapes.add("Credit fiscal genere : " + credit + " Ar");
        } else {
            etapes.add("Solde a payer : " + solde + " Ar");
        }

        return ResultatCalculFiscal.builder()
                .typeImpot("IR")
                .categorieActivite(categorie)
                .baseImposable(base)
                .tauxApplique(taux)
                .montantBrut(irTheorique)
                .montantMinimum(minimum)
                .acomptesDeduits(acomptesNvl)
                .creditFiscal(credit)
                .solde(solde)
                .explication("IR = MAX(base x taux ; minimum) - acomptes")
                .etapesCalcul(etapes)
                .build();
    }

    private BigDecimal determinerTaux(CategorieActivite categorie) {
        if (categorie == CategorieActivite.EDUCATION || categorie == CategorieActivite.SANTE) {
            return TAUX_REDUIT;
        }
        if (categorie == CategorieActivite.CARBURANT_DETAIL) {
            return TAUX_CARBURANT;
        }
        return TAUX_NORMAL;
    }

    private BigDecimal calculerMinimum(BigDecimal caHT, CategorieActivite categorie, int moisActifs) {
        BigDecimal partFixe;
        BigDecimal tauxVariable;

        switch (categorie) {
            case AGRICOLE:
            case ARTISANAL:
            case INDUSTRIEL:
            case MINIER:
            case HOTELIER:
            case TOURISTIQUE:
                partFixe = PART_FIXE_AGRICOLE;
                tauxVariable = new BigDecimal("0.01");
                break;
            case CARBURANT_DETAIL:
                partFixe = BigDecimal.ZERO;
                tauxVariable = TAUX_CARBURANT;
                break;
            case EDUCATION:
            case SANTE:
                partFixe = PART_FIXE_EDUCATION_SANTE;
                tauxVariable = new BigDecimal("0.002");
                break;
            case ASSOCIE_GERANT_SARL:
                return MINIMUM_ASSOCIE_SARL;
            default:
                partFixe = PART_FIXE_AUTRES;
                tauxVariable = new BigDecimal("0.01");
        }

        // Proratisation de la part fixe
        BigDecimal partFixeProratisee = partFixe
                .multiply(BigDecimal.valueOf(moisActifs))
                .divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);

        return partFixeProratisee.add(caHT.multiply(tauxVariable));
    }

    private BigDecimal arrondirMillier(BigDecimal valeur) {
        return valeur.divide(BigDecimal.valueOf(1000), 0, RoundingMode.FLOOR)
                .multiply(BigDecimal.valueOf(1000));
    }

    private BigDecimal nvl(BigDecimal valeur) {
        return valeur == null ? BigDecimal.ZERO : valeur;
    }
}
