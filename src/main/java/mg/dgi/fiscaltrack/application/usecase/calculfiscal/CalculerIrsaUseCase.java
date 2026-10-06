package mg.dgi.fiscaltrack.application.usecase.calculfiscal;

import mg.dgi.fiscaltrack.domain.exception.CalculFiscalInvalideException;
import mg.dgi.fiscaltrack.domain.model.ResultatCalculFiscal;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Calcul de l'Impot sur les Revenus Salariaux et Assimilies (IRSA).
 *
 * Bareme progressif mensuel par tranches.
 * Minimum d'imposition : 3 000 Ar.
 * Reduction pour charge de famille : 2 000 Ar par personne.
 */
@Service
public class CalculerIrsaUseCase {

    private static final BigDecimal[] SEUILS = {
            new BigDecimal("350000"),
            new BigDecimal("400000"),
            new BigDecimal("500000"),
            new BigDecimal("600000")
    };

    private static final BigDecimal[] TAUX = {
            new BigDecimal("0.00"),
            new BigDecimal("0.05"),
            new BigDecimal("0.10"),
            new BigDecimal("0.15"),
            new BigDecimal("0.20")
    };

    private static final BigDecimal MINIMUM_IRSA = new BigDecimal("3000");
    private static final BigDecimal REDUCTION_CHARGE = new BigDecimal("2000");

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

        // Base imposable mensuelle
        BigDecimal base = salaireBrut
                .subtract(nvl(cotisationsSociales))
                .add(nvl(avantageLogement))
                .add(nvl(avantageVehicule))
                .add(nvl(avantageTelephone));

        // Arrondi a la centaine inferieure
        base = arrondirCentaine(base);

        // Calcul du bareme par tranches
        BigDecimal impot = BigDecimal.ZERO;
        BigDecimal reste = base;
        BigDecimal seuilPrecedent = BigDecimal.ZERO;

        for (int i = 0; i < SEUILS.length; i++) {
            BigDecimal seuil = SEUILS[i];
            BigDecimal tranche = seuil.subtract(seuilPrecedent);

            if (reste.signum() <= 0) break;

            BigDecimal montantDansTranche = reste.min(tranche);
            impot = impot.add(montantDansTranche.multiply(TAUX[i]));
            reste = reste.subtract(montantDansTranche);
            seuilPrecedent = seuil;
        }

        // Derniere tranche (> 600 000)
        if (reste.signum() > 0) {
            impot = impot.add(reste.multiply(TAUX[TAUX.length - 1]));
        }

        // Minimum
        BigDecimal minimum = MINIMUM_IRSA;

        // Reduction charge de famille
        BigDecimal reduction = REDUCTION_CHARGE.multiply(BigDecimal.valueOf(nombrePersonnesCharge));

        // IRSA du
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

    private BigDecimal arrondirCentaine(BigDecimal valeur) {
        return valeur.divide(BigDecimal.valueOf(100), 0, RoundingMode.FLOOR)
                .multiply(BigDecimal.valueOf(100));
    }

    private BigDecimal nvl(BigDecimal valeur) {
        return valeur == null ? BigDecimal.ZERO : valeur;
    }
}
