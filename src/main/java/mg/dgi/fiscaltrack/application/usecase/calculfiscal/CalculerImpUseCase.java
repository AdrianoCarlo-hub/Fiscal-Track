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
 * Calcul de l'Impot sur les Marchés Publics (IMP).
 *
 * Regles (CDI 01.01.51, 01.01.15) :
 *   - Taux : 8% (libératoire)
 *   - Acompte : 5 pour mille (0.5%) des marchés aux formalités constitutives
 */
@Service
public class CalculerImpUseCase {

    private final ParametreFiscalService parametreFiscalService;

    public CalculerImpUseCase(ParametreFiscalService parametreFiscalService) {
        this.parametreFiscalService = parametreFiscalService;
    }

    /**
     * Calcule l'IMP sur un marche public.
     * 
     * @param montantMarche le montant du marche
     * @param acomptes les acomptes eventuals
     * @return le resultat du calcul
     */
    public ResultatCalculFiscal execute(BigDecimal montantMarche, BigDecimal acomptes) {
        if (montantMarche == null || montantMarche.signum() < 0) {
            throw new CalculFiscalInvalideException("Le montant du marche est obligatoire et positif");
        }

        // Taux IMP = 8% (CDI 01.01.51)
        BigDecimal tauxImp = parametreFiscalService.getValeur("IMP_TAUX");
        BigDecimal impTheorique = montantMarche.multiply(tauxImp).setScale(2, RoundingMode.HALF_UP);

        // Acomptes
        BigDecimal acomptesNvl = nvl(acomptes);
        BigDecimal solde = impTheorique.subtract(acomptesNvl);

        BigDecimal credit = BigDecimal.ZERO;
        if (solde.signum() < 0) {
            credit = solde.abs();
            solde = BigDecimal.ZERO;
        }

        List<String> etapes = new ArrayList<>();
        etapes.add("Montant du marche : " + montantMarche + " Ar");
        etapes.add("IMP theorique (8%) : " + impTheorique + " Ar");
        etapes.add("Acomptes deduits : " + acomptesNvl + " Ar");
        if (credit.signum() > 0) {
            etapes.add("Credit fiscal genere : " + credit + " Ar");
        } else {
            etapes.add("Solde a payer : " + solde + " Ar");
        }

        return ResultatCalculFiscal.builder()
                .typeImpot("IMP")
                .baseImposable(montantMarche)
                .tauxApplique(tauxImp)
                .montantBrut(impTheorique)
                .acomptesDeduits(acomptesNvl)
                .creditFiscal(credit)
                .solde(solde)
                .explication("IMP = Montant du marche x 8% - acomptes")
                .etapesCalcul(etapes)
                .build();
    }

    /**
     * Calcule l'acompte IMP aux formalites constitutives.
     * 5 pour mille (0.5%) du montant du marche (CDI 01.01.15).
     * 
     * @param montantMarche le montant du marche
     * @return le montant de l'acompte
     */
    public BigDecimal calculerAcompteImp(BigDecimal montantMarche) {
        if (montantMarche == null || montantMarche.signum() < 0) {
            throw new CalculFiscalInvalideException("Le montant du marche est obligatoire et positif");
        }
        // 5 pour mille (CDI 01.01.15)
        BigDecimal tauxAcompte = parametreFiscalService.getValeur("IMP_TAUX_ACOMPTE");
        return montantMarche.multiply(tauxAcompte).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal nvl(BigDecimal valeur) {
        return valeur == null ? BigDecimal.ZERO : valeur;
    }
}
