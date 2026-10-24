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
 * Calcul de la Taxe sur la Valeur Ajoutee (TVA).
 *
 * Taux : 20% (normal), 5% (gaz butane), 0% (exportations) (CDI 06.01.12).
 * TVA nette = TVA collectee - TVA deductible.
 * Si negatif : credit de TVA reportable.
 * Seuil d'assujettissement : 400 000 000 Ar (CDI 06.01.04).
 * Declaration et paiement au plus tard le 15 du mois suivant (CDI 06.01.16).
 */
@Service
public class CalculerTvaUseCase {

    private final ParametreFiscalService parametreFiscalService;

    public CalculerTvaUseCase(ParametreFiscalService parametreFiscalService) {
        this.parametreFiscalService = parametreFiscalService;
    }

    public ResultatCalculFiscal execute(BigDecimal tvaCollectee,
                                          BigDecimal tvaDeductible,
                                          BigDecimal creditTvaAnterieur,
                                          boolean exportation) {
        return execute(tvaCollectee, tvaDeductible, creditTvaAnterieur, exportation, false, null);
    }

    public ResultatCalculFiscal execute(BigDecimal tvaCollectee,
                                          BigDecimal tvaDeductible,
                                          BigDecimal creditTvaAnterieur,
                                          boolean exportation,
                                          boolean gazButane,
                                          BigDecimal chiffreAffaires) {

        if (tvaCollectee == null || tvaCollectee.signum() < 0) {
            throw new CalculFiscalInvalideException("La TVA collectee est obligatoire et positive");
        }
        if (tvaDeductible == null || tvaDeductible.signum() < 0) {
            throw new CalculFiscalInvalideException("La TVA deductibile est obligatoire et positive");
        }

        // Taux applique : 0% export, 5% gaz butane, 20% normal (CDI 06.01.12)
        BigDecimal tauxApplique;
        if (exportation) {
            tauxApplique = parametreFiscalService.getValeur("TVA_TAUX_EXPORT");
        } else if (gazButane) {
            tauxApplique = parametreFiscalService.getValeur("TVA_TAUX_GAZ_BUTANE");
        } else {
            tauxApplique = parametreFiscalService.getValeur("TVA_TAUX_NORMAL");
        }

        // Les exportations sont taxees a 0% : la TVA collectee doit etre ramenee a 0.
        BigDecimal tvaCollecteeRetenue = exportation ? BigDecimal.ZERO : tvaCollectee;

        // TVA nette
        BigDecimal tvaNette = tvaCollecteeRetenue.subtract(tvaDeductible);

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

        List<String> etapes = new ArrayList<>();
        if (exportation) {
            etapes.add("Exportation : TVA collectee ramenee a 0 (taux 0%)");
        } else if (gazButane) {
            etapes.add("Gaz butane : taux reduit 5%");
        }
        etapes.add("TVA collectee : " + tvaCollecteeRetenue + " Ar");
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

        // Controle du seuil d'assujettissement (CDI 06.01.04)
        List<String> avertissements = new ArrayList<>();
        if (chiffreAffaires != null) {
            BigDecimal seuil = parametreFiscalService.getValeur("TVA_SEUIL_ASSUJETTISSEMENT");
            if (chiffreAffaires.signum() >= 0 && chiffreAffaires.compareTo(seuil) < 0) {
                avertissements.add("Chiffre d'affaires (" + chiffreAffaires
                        + " Ar) inferieur au seuil d'assujettissement TVA (" + seuil
                        + " Ar) : contribuable normalement non assujetti (CDI 06.01.04).");
            }
        }

        return ResultatCalculFiscal.builder()
                .typeImpot("TVA")
                .tauxApplique(tauxApplique)
                .baseImposable(tvaCollecteeRetenue)
                .montantBrut(tvaNette)
                .creditFiscal(credit)
                .solde(tvaDue)
                .explication("TVA = TVA collectee - TVA deductibile - credit anterieur")
                .etapesCalcul(etapes)
                .avertissements(avertissements)
                .build();
    }

    private BigDecimal nvl(BigDecimal valeur) {
        return valeur == null ? BigDecimal.ZERO : valeur;
    }
}
