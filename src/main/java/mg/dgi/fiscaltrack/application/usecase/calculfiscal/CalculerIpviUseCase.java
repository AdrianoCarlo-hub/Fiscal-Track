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
 * Calcul de l'Impot sur les Plus-Values Immobilieres (IPVI).
 *
 * Regles (CDI 01.05 et suivants) :
 *   - Prix de revient forfaitaire = 75% du prix de vente
 *   - Donc 25% imposable du prix de vente
 *   - Taux : 20%
 *   - IPVI = 25% du prix de vente x 20% = 5% du prix de vente
 */
@Service
public class CalculerIpviUseCase {

    private final ParametreFiscalService parametreFiscalService;

    public CalculerIpviUseCase(ParametreFiscalService parametreFiscalService) {
        this.parametreFiscalService = parametreFiscalService;
    }

    /**
     * Calcule l'IPVI sur une operation.
     * 
     * @param prixVente le prix de vente
     * @param acomptes les acomptes eventuals
     * @return le resultat du calcul
     */
    public ResultatCalculFiscal execute(BigDecimal prixVente, BigDecimal acomptes) {
        if (prixVente == null || prixVente.signum() < 0) {
            throw new CalculFiscalInvalideException("Le prix de vente est obligatoire et positif");
        }

        // Prix de revient forfaitaire = 75% du prix de vente (CDI 01.05)
        BigDecimal tauxPrixRevenu = parametreFiscalService.getValeur("IPVI_TAUX_PRIX_REVIENT");
        BigDecimal prixRevenu = prixVente.multiply(tauxPrixRevenu);

        // Base imposable = Prix de vente - Prix de revenu = 25% du prix de vente
        BigDecimal baseImposable = prixVente.subtract(prixRevenu);

        // Taux IPVI = 20% (CDI 01.05)
        BigDecimal tauxIpvi = parametreFiscalService.getValeur("IPVI_TAUX");
        BigDecimal ipviTheorique = baseImposable.multiply(tauxIpvi).setScale(2, RoundingMode.HALF_UP);

        // Acomptes
        BigDecimal acomptesNvl = nvl(acomptes);
        BigDecimal solde = ipviTheorique.subtract(acomptesNvl);

        BigDecimal credit = BigDecimal.ZERO;
        if (solde.signum() < 0) {
            credit = solde.abs();
            solde = BigDecimal.ZERO;
        }

        List<String> etapes = new ArrayList<>();
        etapes.add("Prix de vente : " + prixVente + " Ar");
        etapes.add("Prix de revient forfaitaire (" + (tauxPrixRevenu.multiply(new BigDecimal(100))) + "%) : " + prixRevenu + " Ar");
        etapes.add("Base imposable (25%) : " + baseImposable + " Ar");
        etapes.add("IPVI theorique (" + (tauxIpvi.multiply(new BigDecimal(100))) + "%) : " + ipviTheorique + " Ar");
        etapes.add("Acomptes deduits : " + acomptesNvl + " Ar");
        if (credit.signum() > 0) {
            etapes.add("Credit fiscal genere : " + credit + " Ar");
        } else {
            etapes.add("Solde a payer : " + solde + " Ar");
        }

        return ResultatCalculFiscal.builder()
                .typeImpot("IPVI")
                .baseImposable(baseImposable)
                .tauxApplique(tauxIpvi)
                .montantBrut(ipviTheorique)
                .acomptesDeduits(acomptesNvl)
                .creditFiscal(credit)
                .solde(solde)
                .explication("IPVI = (Prix de vente x " + ((new BigDecimal("1").subtract(tauxPrixRevenu)).multiply(new BigDecimal(100))) + "%) x " + (tauxIpvi.multiply(new BigDecimal(100))) + "% - acomptes")
                .etapesCalcul(etapes)
                .build();
    }

    private BigDecimal nvl(BigDecimal valeur) {
        return valeur == null ? BigDecimal.ZERO : valeur;
    }
}
