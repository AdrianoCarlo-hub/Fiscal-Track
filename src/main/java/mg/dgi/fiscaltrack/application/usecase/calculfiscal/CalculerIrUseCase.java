package mg.dgi.fiscaltrack.application.usecase.calculfiscal;

import mg.dgi.fiscaltrack.application.usecase.admin.ParametreFiscalService;
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
 * Regles (CDI 01.01.05, 01.01.14 V, 01.01.15) :
 *   - Taux normal : 20% (CDI 01.01.05)
 *   - Vente de carburant au detail : 20% sur le benefice, mais MINIMUM de
 *     perception de 2 pour mille du CA (CDI 01.01.05 VI). Le 2 pour mille n'est
 *     PAS un taux d'IR sur le benefice.
 *   - Education / sante : 10% et minimum de 200 000 Ar + 2 pour mille du CA,
 *     SEULEMENT si l'autorisation d'ouverture est renseignee ; pour la sante,
 *     en plus seulement sur option (CDI 01.01.05 V, 01.01.14 V). Sans
 *     autorisation : regime general (20% et minimum de la categorie AUTRES).
 *   - Minimum selon categorie d'activite (CDI 01.01.05)
 *   - Deficits reportables : report sur 5 exercices, plus anciens d'abord.
 *     Le minimum de perception reste du en cas de deficit.
 *   - Arrondi au millier inferieur
 */
@Service
public class CalculerIrUseCase {

    private final ParametreFiscalService parametreFiscalService;

    public CalculerIrUseCase(ParametreFiscalService parametreFiscalService) {
        this.parametreFiscalService = parametreFiscalService;
    }

    public ResultatCalculFiscal execute(BigDecimal caHT,
                                          BigDecimal chargesDeductibles,
                                          BigDecimal reintegrations,
                                          BigDecimal deductions,
                                          BigDecimal deficitsReportables,
                                          CategorieActivite categorie,
                                          BigDecimal acomptes,
                                          int moisActifs,
                                          boolean autorisationOuverture,
                                          boolean optionSante) {

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

        // Regime reduit education / sante uniquement si autorisation (et option sante)
        boolean regimeReduit = estRegimeEducationSanteReduit(categorie, autorisationOuverture, optionSante);

        // Calcul de la base imposable
        BigDecimal base = caHT
                .subtract(nvl(chargesDeductibles))
                .add(nvl(reintegrations))
                .subtract(nvl(deductions))
                .subtract(nvl(deficitsReportables));

        // Abattement special pour associe-gerant SARL (CDI 01.01.05)
        if (categorie == CategorieActivite.ASSOCIE_GERANT_SARL) {
            BigDecimal abattement = parametreFiscalService.getValeur("IR_ABATTEMENT_SARL");
            base = base.subtract(abattement);
            if (base.signum() < 0) base = BigDecimal.ZERO;
        }

        // Arrondi au millier inferieur (CDI 01.01.05)
        base = arrondirMillier(base);

        // Determination du taux
        BigDecimal taux = determinerTaux(categorie, regimeReduit);

        // IR theorique
        BigDecimal irTheorique = base.multiply(taux).setScale(2, RoundingMode.HALF_UP);

        // Minimum de perception (CDI 01.01.05) : reste du en cas de deficit
        BigDecimal minimum = calculerMinimum(caHT, categorie, moisActifs, regimeReduit);

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

    /**
     * Le regime reduit education/sante (10% et minimum reduit) n'est applicable
     * que si l'autorisation d'ouverture est renseignee (CDI 01.01.05 V,
     * 01.01.14 V). Pour la sante, il faut en plus une option explicite.
     */
    private boolean estRegimeEducationSanteReduit(CategorieActivite categorie,
                                                  boolean autorisationOuverture,
                                                  boolean optionSante) {
        if (categorie == CategorieActivite.EDUCATION) {
            return autorisationOuverture;
        }
        if (categorie == CategorieActivite.SANTE) {
            return autorisationOuverture && optionSante;
        }
        return false;
    }

    /**
     * Determination du taux selon la categorie (CDI 01.01.05, 01.01.14 V, 01.01.15).
     *   - EDUCATION, SANTE (sous autorisation/option) : 10%
     *   - Toutes les autres (dont CARBURANT_DETAIL) : 20%
     *
     * La vente de carburant au detail est imposee a 20% sur le benefice ; le
     * 2 pour mille du CA est un MINIMUM de perception, pas un taux d'IR.
     */
    private BigDecimal determinerTaux(CategorieActivite categorie, boolean regimeReduit) {
        if (regimeReduit) {
            return parametreFiscalService.getValeur("IR_TAUX_EDUCATION_SANTE");
        }
        return parametreFiscalService.getValeur("IR_TAUX_NORMAL");
    }

    /**
     * Calcul du minimum de perception selon la categorie (CDI 01.01.05).
     *   - AGRICOLE, ARTISANAL, INDUSTRIEL, MINIER, HOTELIER, TOURISTIQUE : 500 000 + 1% du CA
     *   - CARBURANT_DETAIL : 0 + 2 pour mille du CA
     *   - EDUCATION, SANTE (sous autorisation/option) : 200 000 + 2 pour mille du CA
     *   - EDUCATION, SANTE sans autorisation : minimum de la categorie AUTRES
     *   - ASSOCIE_GERANT_SARL : 320 000
     *   - AUTRES : 1 000 000 + 1% du CA
     */
    private BigDecimal calculerMinimum(BigDecimal caHT, CategorieActivite categorie,
                                       int moisActifs, boolean regimeReduit) {
        BigDecimal partFixe;
        BigDecimal tauxVariable;

        if (regimeReduit) {
            partFixe = parametreFiscalService.getValeur("IR_PART_FIXE_EDUCATION_SANTE");
            tauxVariable = parametreFiscalService.getValeur("IR_TAUX_VARIABLE_EDUCATION");
        } else {
            switch (categorie) {
                case AGRICOLE:
                case ARTISANAL:
                case INDUSTRIEL:
                case MINIER:
                case HOTELIER:
                case TOURISTIQUE:
                    partFixe = parametreFiscalService.getValeur("IR_PART_FIXE_AGRICOLE");
                    tauxVariable = parametreFiscalService.getValeur("IR_TAUX_VARIABLE_AGRICOLE");
                    break;
                case CARBURANT_DETAIL:
                    // Minimum de perception : 2 pour mille du CA (CDI 01.01.05 VI)
                    partFixe = BigDecimal.ZERO;
                    tauxVariable = parametreFiscalService.getValeur("IR_TAUX_CARBURANT");
                    break;
                case ASSOCIE_GERANT_SARL:
                    return parametreFiscalService.getValeur("IR_MINIMUM_SARL");
                default:
                    // EDUCATION/SANTE sans autorisation, et toutes les autres categories
                    partFixe = parametreFiscalService.getValeur("IR_PART_FIXE_AUTRES");
                    tauxVariable = parametreFiscalService.getValeur("IR_TAUX_VARIABLE_AGRICOLE");
            }
        }

        // Proratisation de la part fixe
        BigDecimal partFixeProratisee = partFixe
                .multiply(BigDecimal.valueOf(moisActifs))
                .divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);

        return partFixeProratisee.add(caHT.multiply(tauxVariable));
    }

    /**
     * Arrondi au millier inferieur (CDI 01.01.05).
     */
    private BigDecimal arrondirMillier(BigDecimal valeur) {
        return valeur.divide(BigDecimal.valueOf(1000), 0, RoundingMode.FLOOR)
                .multiply(BigDecimal.valueOf(1000));
    }

    private BigDecimal nvl(BigDecimal valeur) {
        return valeur == null ? BigDecimal.ZERO : valeur;
    }
}
