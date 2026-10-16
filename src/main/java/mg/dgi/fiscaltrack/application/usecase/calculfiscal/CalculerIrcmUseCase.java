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
 * Calcul de l'Impot sur les Revenus des Collectivites et des Societes Mutualistes (IRCM).
 *
 * Regles :
 *   - Taux normal : 20%
 *   - Taux reduit pour cooperatives : 10%
 */
@Service
public class CalculerIrcmUseCase {

    private final ParametreFiscalService parametreFiscalService;

    public CalculerIrcmUseCase(ParametreFiscalService parametreFiscalService) {
        this.parametreFiscalService = parametreFiscalService;
    }

    /**
     * Calcule l'IRCM.
     * 
     * @param caBrut le chiffre d'affaires brut
     * @param estCooperative true si le contribuable est une cooperative
     * @param acomptes les acomptes eventuals
     * @return le resultat du calcul
     */
    public ResultatCalculFiscal execute(BigDecimal caBrut, boolean estCooperative, BigDecimal acomptes) {
        if (caBrut == null || caBrut.signum() < 0) {
            throw new CalculFiscalInvalideException("Le chiffre d'affaires brut est obligatoire et positif");
        }

        // Taux selon type de contribuable
        BigDecimal taux = estCooperative 
                ? parametreFiscalService.getValeur("IRCM_TAUX_COOPERATIVE")  // Taux reduit pour cooperatives
                : parametreFiscalService.getValeur("IRCM_TAUX_NORMAL");  // Taux normal

        BigDecimal ircmTheorique = caBrut.multiply(taux).setScale(2, RoundingMode.HALF_UP);

        // Acomptes
        BigDecimal acomptesNvl = nvl(acomptes);
        BigDecimal solde = ircmTheorique.subtract(acomptesNvl);

        BigDecimal credit = BigDecimal.ZERO;
        if (solde.signum() < 0) {
            credit = solde.abs();
            solde = BigDecimal.ZERO;
        }

        List<String> etapes = new ArrayList<>();
        etapes.add("CA brut : " + caBrut + " Ar");
        etapes.add("Taux applique : " + (taux.multiply(new BigDecimal(100))) + "%");
        etapes.add("IRCM theorique : " + ircmTheorique + " Ar");
        etapes.add("Acomptes deduits : " + acomptesNvl + " Ar");
        if (credit.signum() > 0) {
            etapes.add("Credit fiscal genere : " + credit + " Ar");
        } else {
            etapes.add("Solde a payer : " + solde + " Ar");
        }

        return ResultatCalculFiscal.builder()
                .typeImpot("IRCM")
                .baseImposable(caBrut)
                .tauxApplique(taux)
                .montantBrut(ircmTheorique)
                .acomptesDeduits(acomptesNvl)
                .creditFiscal(credit)
                .solde(solde)
                .explication("IRCM = CA x " + (taux.multiply(new BigDecimal(100))) + "% - acomptes")
                .etapesCalcul(etapes)
                .build();
    }

    private BigDecimal nvl(BigDecimal valeur) {
        return valeur == null ? BigDecimal.ZERO : valeur;
    }
}
