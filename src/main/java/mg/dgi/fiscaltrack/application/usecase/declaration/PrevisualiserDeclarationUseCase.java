package mg.dgi.fiscaltrack.application.usecase.declaration;

import mg.dgi.fiscaltrack.application.port.out.ObligationFiscaleRepositoryPort;
import mg.dgi.fiscaltrack.domain.exception.AccesRefuseException;
import mg.dgi.fiscaltrack.domain.exception.ObligationIntrouvableException;
import mg.dgi.fiscaltrack.domain.model.ObligationFiscale;
import mg.dgi.fiscaltrack.domain.model.ResultatCalculFiscal;
import mg.dgi.fiscaltrack.domain.model.SaisieDeclaration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Previsualisation de l'impot calcule par le serveur pour une obligation,
 * sans persistance. Le frontend affiche ce montant (champ non libre) avant
 * le depot reel de la declaration (BLOC 4.7).
 *
 * Depuis « formulaires-et-ux », la previsualisation recoit les champs du
 * formulaire adapte au type d'impot : ils sont valides puis transmis au
 * dispatcher inchange. Un type sans formulaire ou un champ manquant leve une
 * exception avec un message en francais (jamais de reponse vide).
 */
@Service
public class PrevisualiserDeclarationUseCase {

    private final ObligationFiscaleRepositoryPort obligationRepositoryPort;
    private final CalculerImpotDeclarationUseCase calculerImpotDeclarationUseCase;
    private final PreparerDonneesDeclarationUseCase preparerDonneesDeclarationUseCase;

    /**
     * Constructeur utilise par Spring et par la previsualisation adaptee a
     * chaque type d'impot.
     */
    @Autowired
    public PrevisualiserDeclarationUseCase(ObligationFiscaleRepositoryPort obligationRepositoryPort,
                                           CalculerImpotDeclarationUseCase calculerImpotDeclarationUseCase,
                                           PreparerDonneesDeclarationUseCase preparerDonneesDeclarationUseCase) {
        this.obligationRepositoryPort = obligationRepositoryPort;
        this.calculerImpotDeclarationUseCase = calculerImpotDeclarationUseCase;
        this.preparerDonneesDeclarationUseCase = preparerDonneesDeclarationUseCase;
    }

    /**
     * Constructeur historique (previsualisation au chiffre d'affaires seul) :
     * conserve pour les appels {@link #execute(Long, BigDecimal)}.
     */
    public PrevisualiserDeclarationUseCase(ObligationFiscaleRepositoryPort obligationRepositoryPort,
                                           CalculerImpotDeclarationUseCase calculerImpotDeclarationUseCase) {
        this(obligationRepositoryPort, calculerImpotDeclarationUseCase, null);
    }

    /**
     * Previsualisation a partir du formulaire adapte a l'impot.
     *
     * @param saisie       champs saisis par le contribuable
     * @param nifAppelant  NIF de l'utilisateur connecte ; null pour un agent.
     *                     Si renseigne, il doit correspondre au NIF de
     *                     l'obligation (aucune donnee d'un autre contribuable).
     */
    public Map<String, Object> previsualiser(SaisieDeclaration saisie, String nifAppelant) {
        ObligationFiscale obligation = obligationRepositoryPort
                .findById(saisie.getIdObligationFiscale())
                .orElseThrow(() -> new ObligationIntrouvableException(saisie.getIdObligationFiscale()));

        verifierPropriete(obligation, nifAppelant);

        PreparerDonneesDeclarationUseCase.DonneesDeclaration donnees =
                preparerDonneesDeclarationUseCase.execute(saisie, obligation);

        Map<String, Object> reponse = base(obligation, donnees.chiffreAffaires());
        reponse.put("detailAcomptes", donnees.detailAcomptes());

        Optional<ResultatCalculFiscal> resultat = calculerImpotDeclarationUseCase
                .executeAvecParametres(obligation.getCodeImpot(), donnees.params(), obligation.getNif());

        if (resultat.isPresent()) {
            completerAvecResultat(reponse, resultat.get());
        } else {
            reponse.put("explication", messageIndisponibilite(obligation.getCodeImpot()));
        }
        return reponse;
    }

    /**
     * Previsualisation historique (ancien contrat : un seul chiffre d'affaires).
     * Conserve pour les clients et les tests existants.
     */
    public Map<String, Object> execute(Long idObligation, BigDecimal chiffreAffaires) {
        ObligationFiscale obligation = obligationRepositoryPort.findById(idObligation)
                .orElseThrow(() -> new ObligationIntrouvableException(idObligation));

        if (chiffreAffaires == null || chiffreAffaires.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Le chiffre d'affaires ne peut pas etre negatif");
        }

        Map<String, Object> reponse = base(obligation, chiffreAffaires);

        Optional<ResultatCalculFiscal> resultat = calculerImpotDeclarationUseCase.execute(
                obligation.getCodeImpot(), chiffreAffaires, obligation.getNif());

        if (resultat.isPresent()) {
            completerAvecResultat(reponse, resultat.get());
        } else {
            reponse.put("explication", messageIndisponibilite(obligation.getCodeImpot()));
        }
        return reponse;
    }

    /**
     * Controle de propriete : un contribuable ne peut consulter ni preparer
     * une declaration que pour son propre NIF.
     */
    private void verifierPropriete(ObligationFiscale obligation, String nifAppelant) {
        if (nifAppelant != null && !nifAppelant.equals(obligation.getNif())) {
            throw new AccesRefuseException(
                    "Acces refuse : cette obligation fiscale appartient a un autre contribuable.");
        }
    }

    private Map<String, Object> base(ObligationFiscale obligation, BigDecimal chiffreAffaires) {
        Map<String, Object> reponse = new LinkedHashMap<>();
        reponse.put("codeImpot", obligation.getCodeImpot());
        reponse.put("nif", obligation.getNif());
        reponse.put("periodeFiscale", obligation.getPeriodeFiscale());
        reponse.put("dateLimiteReelle", obligation.getDateLimiteReelle());
        reponse.put("chiffreAffairesDeclare", chiffreAffaires);
        reponse.put("disponible", false);
        reponse.put("impotRetenu", BigDecimal.ZERO);
        reponse.put("creditFiscal", BigDecimal.ZERO);
        reponse.put("baseImposable", null);
        reponse.put("tauxApplique", null);
        reponse.put("montantBrut", null);
        reponse.put("montantMinimum", null);
        reponse.put("explication", "Aucun calcul disponible pour ce type d'impot.");
        reponse.put("etapesCalcul", List.of());
        reponse.put("avertissements", List.of());
        return reponse;
    }

    private void completerAvecResultat(Map<String, Object> reponse, ResultatCalculFiscal r) {
        reponse.put("disponible", true);
        reponse.put("impotRetenu", r.getSolde() == null ? BigDecimal.ZERO : r.getSolde());
        reponse.put("creditFiscal", r.getCreditFiscal() == null ? BigDecimal.ZERO : r.getCreditFiscal());
        reponse.put("baseImposable", r.getBaseImposable());
        reponse.put("tauxApplique", r.getTauxApplique());
        reponse.put("montantBrut", r.getMontantBrut());
        reponse.put("montantMinimum", r.getMontantMinimum());
        reponse.put("reductions", r.getReductions());
        reponse.put("acomptesDeduits", r.getAcomptesDeduits());
        reponse.put("explication", r.getExplication());
        reponse.put("etapesCalcul", r.getEtapesCalcul());
        reponse.put("avertissements", r.getAvertissements());
    }

    /** Message explicite quand aucun montant n'est calculable. */
    private String messageIndisponibilite(String codeImpot) {
        if ("ACOMPTE_IR".equalsIgnoreCase(codeImpot)) {
            return "Aucune declaration IR anterieure : aucun acompte.";
        }
        return "Aucun calcul disponible pour le type d'impot " + codeImpot
                + ". Contactez votre agent du centre fiscal.";
    }
}
