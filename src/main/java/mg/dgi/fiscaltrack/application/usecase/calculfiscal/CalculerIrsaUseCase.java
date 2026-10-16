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
 * Calcul de l'Impot sur les Revenus Salariaux et Assimilés (IRSA).
 *
 * Bareme progressif mensuel par tranches (CDI 01.03.16, 01.03.19) :
 *   - Tranche 1 : <= 350 000 Ar, taux 0%
 *   - Tranche 2 : 350 001 - 400 000 Ar, taux 5%
 *   - Tranche 3 : 400 001 - 500 000 Ar, taux 10%
 *   - Tranche 4 : 500 001 - 600 000 Ar, taux 15%
 *   - Tranche 5 : > 600 000 Ar, taux 20%
 * Minimum d'imposition : 3 000 Ar.
 * Reduction pour charge de famille : 2 000 Ar par personne.
 * Base = salaire brut - cotisations + avantages en nature.
 * Arrondi a la centaine inferieure (01.03.16 et 01.03.19).
 */
@Service
public class CalculerIrsaUseCase {

    private final ParametreFiscalService parametreFiscalService;

    public CalculerIrsaUseCase(ParametreFiscalService parametreFiscalService) {
        this.parametreFiscalService = parametreFiscalService;
    }

    public ResultatCalculFiscal execute(BigDecimal salaireBrut,
                                          BigDecimal cotisationsSociales,
                                          BigDecimal avantageLogement,
                                          BigDecimal avantageVehicule,
                                          BigDecimal avantageTelephone,
                                          int nombrePersonnesCharge) {

        if (salaireBrut == null || salaireBrut.signum() < 0) {
            throw new CalculFiscalInvalideException("Le salaire brut est obligatoire et positif");
        }
        if (nombrePersonnesCharge < 0) {
            throw new CalculFiscalInvalideException("Le nombre de personnes a charge ne peut pas etre negatif");
        }

        // Base imposable mensuelle (CDI 01.03.16)
        BigDecimal base = salaireBrut
                .subtract(nvl(cotisationsSociales))
                .add(nvl(avantageLogement))
                .add(nvl(avantageVehicule))
                .add(nvl(avantageTelephone));

        // Arrondi a la centaine inferieure (CDI 01.03.16, 01.03.19)
        base = arrondirCentaine(base);

        // Calcul du bareme par tranches
        BigDecimal[] seuils = getSeuilsTranches();
        BigDecimal[] taux = getTauxTranches();
        
        BigDecimal impot = BigDecimal.ZERO;
        BigDecimal reste = base;
        BigDecimal seuilPrecedent = BigDecimal.ZERO;

        for (int i = 0; i < seuils.length; i++) {
            BigDecimal seuil = seuils[i];
            BigDecimal tranche = seuil.subtract(seuilPrecedent);

            if (reste.signum() <= 0) break;

            BigDecimal montantDansTranche = reste.min(tranche);
            impot = impot.add(montantDansTranche.multiply(taux[i]));
            reste = reste.subtract(montantDansTranche);
            seuilPrecedent = seuil;
        }

        // Derniere tranche (> 600 000)
        if (reste.signum() > 0) {
            impot = impot.add(reste.multiply(taux[taux.length - 1]));
        }

        // Minimum (CDI 01.03.16)
        BigDecimal minimum = parametreFiscalService.getValeur("IRSA_MINIMUM");

        // Reduction charge de famille (CDI 01.03.19)
        BigDecimal reductionParPersonne = parametreFiscalService.getValeur("IRSA_REDUCTION_PAR_PERSONNE");
        BigDecimal reduction = reductionParPersonne.multiply(BigDecimal.valueOf(nombrePersonnesCharge));

        // IRSA du
        // NOTE : l'ordre relatif entre le plancher (minimum de perception) et la
        // reduction pour charges n'est pas verifiable (CDI indisponible). Comportement
        // retenu : plancher applique d'abord, puis reduction (peut descendre sous le
        // plancher). Question ouverte : QUESTIONS_ENCADREUR.md.
        BigDecimal irsaDu = impot.max(minimum).subtract(reduction);
        if (irsaDu.signum() < 0) irsaDu = BigDecimal.ZERO;

        List<String> etapes = new ArrayList<>();
        etapes.add("Base imposable mensuelle : " + base + " Ar");
        etapes.add("Impot selon bareme : " + impot.setScale(2, RoundingMode.HALF_UP) + " Ar");
        etapes.add("Minimum IRSA : " + minimum + " Ar");
        etapes.add("Reduction charges (" + nombrePersonnesCharge + " pers.) : -" + reduction + " Ar");
        etapes.add("IRSA a payer : " + irsaDu + " Ar");

        return ResultatCalculFiscal.builder()
                .typeImpot("IRSA")
                .baseImposable(base)
                .montantBrut(impot.setScale(2, RoundingMode.HALF_UP))
                .montantMinimum(minimum)
                .reductions(reduction)
                .solde(irsaDu)
                .explication("IRSA = MAX(bareme ; 3 000) - (2 000 x personnes a charge)")
                .etapesCalcul(etapes)
                .build();
    }

    /**
     * Recupere les seuils des tranches IRSA (CDI 01.03.16).
     */
    private BigDecimal[] getSeuilsTranches() {
        return new BigDecimal[] {
            parametreFiscalService.getValeur("IRSA_SEUIL_1"),
            parametreFiscalService.getValeur("IRSA_SEUIL_2"),
            parametreFiscalService.getValeur("IRSA_SEUIL_3"),
            parametreFiscalService.getValeur("IRSA_SEUIL_4")
        };
    }

    /**
     * Recupere les taux des tranches IRSA (CDI 01.03.16).
     */
    private BigDecimal[] getTauxTranches() {
        return new BigDecimal[] {
            parametreFiscalService.getValeur("IRSA_TAUX_0"),
            parametreFiscalService.getValeur("IRSA_TAUX_1"),
            parametreFiscalService.getValeur("IRSA_TAUX_2"),
            parametreFiscalService.getValeur("IRSA_TAUX_3"),
            parametreFiscalService.getValeur("IRSA_TAUX_4")
        };
    }

    /**
     * Arrondi a la centaine inferieure (CDI 01.03.16, 01.03.19).
     */
    private BigDecimal arrondirCentaine(BigDecimal valeur) {
        return valeur.divide(BigDecimal.valueOf(100), 0, RoundingMode.FLOOR)
                .multiply(BigDecimal.valueOf(100));
    }

    private BigDecimal nvl(BigDecimal valeur) {
        return valeur == null ? BigDecimal.ZERO : valeur;
    }
}
