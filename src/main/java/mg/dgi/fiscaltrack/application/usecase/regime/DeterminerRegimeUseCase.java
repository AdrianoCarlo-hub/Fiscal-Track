package mg.dgi.fiscaltrack.application.usecase.regime;

import mg.dgi.fiscaltrack.application.usecase.admin.ParametreFiscalService;
import mg.dgi.fiscaltrack.domain.model.ParametreFiscal;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Service pour avertir sur les seuils d'assujettissement.
 *
 * Les seuils IR (20 M) et IS synthetique (200 M) sont en conflit entre sources
 * (CDI vs documents 2026) : ils ne sont PAS codies comme regles de regime.
 * Seul le seuil TVA (400 M) est confirme par CDI 01.01.13 et 06.01.04.
 *
 * Ce service ne determine PAS le regime (pas de changement automatique) :
 * il emet des AVERTISSEMENTS lorsqu'un seuil confirme est franchi, ou lorsqu'un
 * parametre explicitement marque « À CONFIRMER » est franchi.
 */
@Service
public class DeterminerRegimeUseCase {

    private final ParametreFiscalService parametreFiscalService;

    public DeterminerRegimeUseCase(ParametreFiscalService parametreFiscalService) {
        this.parametreFiscalService = parametreFiscalService;
    }

    /**
     * Vérifie si le CA dépasse le seuil TVA confirmé (400M) et émet un avertissement,
     * puis signale tout paramètre marqué « À CONFIRMER » franchi (sans changer de régime).
     * CDI 01.01.13 et 06.01.04 : seuil réel/TVA = 400 000 000 Ar.
     *
     * @param chiffreAffaires le CA du contribuable
     * @return liste d'avertissements, jamais vide
     */
    public List<String> verifierAvertissements(BigDecimal chiffreAffaires) {
        List<String> avertissements = new ArrayList<>();

        if (chiffreAffaires == null) {
            avertissements.add("AVERTISSEMENT : Le chiffre d'affaires n'est pas renseigné. " +
                    "Vérifiez les données du contribuable.");
            return avertissements;
        }

        // Seuil TVA confirmé : 400M (CDI 01.01.13 et 06.01.04)
        BigDecimal seuilTva = parametreFiscalService.getValeur("REGIME_TVA_SEUIL");
        if (chiffreAffaires.compareTo(seuilTva) >= 0) {
            avertissements.add("AVERTISSEMENT : CA >= " + seuilTva + " Ar - " +
                    "Seuil TVA confirmé (CDI 01.01.13 / 06.01.04). " +
                    "Vérifiez l'assujettissement TVA.");
        }

        // Les seuils de régime en conflit (20 M / 200 M) ne sont codies nulle part :
        // seuls des paramètres explicitement marqués « À CONFIRMER » produisent un
        // avertissement, sans blocage ni changement automatique de régime.
        for (ParametreFiscal parametre : parametreFiscalService.getParametresAConfirmer()) {
            if (parametre.getValeur() != null
                    && chiffreAffaires.compareTo(parametre.getValeur()) >= 0) {
                avertissements.add("AVERTISSEMENT : seuil " + parametre.getCode() + " de "
                        + parametre.getValeur() + " Ar marqué « " + parametre.getBaseLegale()
                        + " »; aucun changement automatique de régime.");
            }
        }

        if (avertissements.isEmpty()) {
            avertissements.add("Aucun avertissement de seuil.");
        }

        return avertissements;
    }

    /**
     * Vérifie si le contribuable est assujetti à la TVA (seul seuil confirmé).
     * CDI 01.01.13 et 06.01.04 : TVA à partir de 400 000 000 Ar.
     * 
     * @param chiffreAffaires le CA du contribuable
     * @return true si assujetti à la TVA, false sinon ou si CA inconnu
     */
    public boolean estAssujettiTva(BigDecimal chiffreAffaires) {
        if (chiffreAffaires == null) {
            return false;
        }
        
        BigDecimal seuilTva = parametreFiscalService.getValeur("REGIME_TVA_SEUIL");
        return chiffreAffaires.compareTo(seuilTva) >= 0;
    }
}
