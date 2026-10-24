package mg.dgi.fiscaltrack.application.usecase.regime;

import mg.dgi.fiscaltrack.application.usecase.admin.ParametreFiscalService;
import mg.dgi.fiscaltrack.domain.enums.RegimeImposition;
import mg.dgi.fiscaltrack.domain.model.ParametreFiscal;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Service pour avertir sur les seuils d'assujettissement.
 *
 * Seul le seuil 400 M est confirme par CDI 01.01.13 / 06.01.04.
 * Le seuil IR 20 M n'est PAS code (sources en conflit).
 *
 * Ce service ne determine PAS le regime (pas de changement automatique) :
 * il emet des AVERTISSEMENTS lorsqu'un seuil confirme est franchi, ou lorsqu'un
 * parametre explicitement marque « À CONFIRMER » est franchi.
 */
@Service
public class DeterminerRegimeUseCase {

    /**
     * Seuil regime synthétique : CA > 200 000 000 Ar -> avertissement de
     * declaration de changement de regime avant la cloture, effet a l'exercice
     * suivant (CPF I-19). Aucun changement automatique de regime.
     */
    static final BigDecimal SEUIL_REGIME_SYNTHETIQUE_CA = new BigDecimal("200000000");

    private final ParametreFiscalService parametreFiscalService;

    public DeterminerRegimeUseCase(ParametreFiscalService parametreFiscalService) {
        this.parametreFiscalService = parametreFiscalService;
    }

    /**
     * Vérifie si le CA dépasse le seuil confirmé (400M) et émet un avertissement,
     * puis signale tout paramètre marqué « À CONFIRMER » franchi (sans changer de régime).
     * CDI 01.01.13 et 06.01.04 : seuil réel/TVA = 400 000 000 Ar.
     *
     * @param chiffreAffaires le CA du contribuable
     * @return liste d'avertissements, jamais vide
     */
    public List<String> verifierAvertissements(BigDecimal chiffreAffaires) {
        return verifierAvertissements(chiffreAffaires, null);
    }

    /**
     * Comme {@link #verifierAvertissements(BigDecimal)}, mais connait le regime
     * du contribuable pour signaler le seuil synthétique 200 M (CPF I-19).
     * Le regime n'est JAMAIS modifie : seul un avertissement est produit.
     *
     * @param chiffreAffaires le CA du contribuable
     * @param regime le regime du contribuable, ou null si inconnu
     * @return liste d'avertissements, jamais vide
     */
    public List<String> verifierAvertissements(BigDecimal chiffreAffaires, RegimeImposition regime) {
        List<String> avertissements = new ArrayList<>();

        if (chiffreAffaires == null) {
            avertissements.add("AVERTISSEMENT : Le chiffre d'affaires n'est pas renseigné. " +
                    "Vérifiez les données du contribuable.");
            return avertissements;
        }

        // Seuil confirmé : 400M (CDI 01.01.13 et 06.01.04) -> regime du reel applicable
        BigDecimal seuilTva = parametreFiscalService.getValeur("REGIME_TVA_SEUIL");
        if (chiffreAffaires.compareTo(seuilTva) >= 0) {
            avertissements.add("AVERTISSEMENT : CA >= " + seuilTva + " Ar - " +
                    "régime du réel applicable (CDI 01.01.13). " +
                    "Seuil TVA confirmé (06.01.04) : vérifiez l'assujettissement TVA.");
        }

        // Seuil synthétique 200 M (CPF I-19) : avertissement seulement, sans
        // changement automatique de régime. Le seuil IR 20 M n'est pas code.
        if (regime == RegimeImposition.SYNTHETIQUE
                && chiffreAffaires.compareTo(SEUIL_REGIME_SYNTHETIQUE_CA) > 0) {
            avertissements.add("AVERTISSEMENT : régime synthétique et CA > "
                    + SEUIL_REGIME_SYNTHETIQUE_CA + " Ar - déclaration de changement de " +
                    "régime avant la clôture, effet à l'exercice suivant (CPF I-19). " +
                    "Aucun changement automatique de régime.");
        }

        // Les autres paramètres explicitement marqués « À CONFIRMER » produisent
        // un avertissement, sans blocage ni changement automatique de régime.
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
