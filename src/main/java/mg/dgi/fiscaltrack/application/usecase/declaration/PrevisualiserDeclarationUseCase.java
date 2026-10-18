package mg.dgi.fiscaltrack.application.usecase.declaration;

import mg.dgi.fiscaltrack.application.port.out.ObligationFiscaleRepositoryPort;
import mg.dgi.fiscaltrack.domain.exception.ObligationIntrouvableException;
import mg.dgi.fiscaltrack.domain.model.ObligationFiscale;
import mg.dgi.fiscaltrack.domain.model.ResultatCalculFiscal;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Previsualisation de l'impot calcule par le serveur pour une obligation,
 * sans persistance. Le frontend affiche ce montant (champ non libre) avant
 * le depot reel de la declaration (BLOC 4.7).
 */
@Service
public class PrevisualiserDeclarationUseCase {

    private final ObligationFiscaleRepositoryPort obligationRepositoryPort;
    private final CalculerImpotDeclarationUseCase calculerImpotDeclarationUseCase;

    public PrevisualiserDeclarationUseCase(ObligationFiscaleRepositoryPort obligationRepositoryPort,
                                           CalculerImpotDeclarationUseCase calculerImpotDeclarationUseCase) {
        this.obligationRepositoryPort = obligationRepositoryPort;
        this.calculerImpotDeclarationUseCase = calculerImpotDeclarationUseCase;
    }

    public Map<String, Object> execute(Long idObligation, BigDecimal chiffreAffaires) {
        ObligationFiscale obligation = obligationRepositoryPort.findById(idObligation)
                .orElseThrow(() -> new ObligationIntrouvableException(idObligation));

        if (chiffreAffaires == null || chiffreAffaires.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Le chiffre d'affaires ne peut pas etre negatif");
        }

        Map<String, Object> reponse = new LinkedHashMap<>();
        reponse.put("codeImpot", obligation.getCodeImpot());
        reponse.put("chiffreAffairesDeclare", chiffreAffaires);
        reponse.put("disponible", false);
        reponse.put("impotRetenu", BigDecimal.ZERO);
        reponse.put("baseImposable", null);
        reponse.put("tauxApplique", null);
        reponse.put("montantBrut", null);
        reponse.put("montantMinimum", null);
        reponse.put("explication", "Aucun calcul disponible pour ce type d'impot.");
        reponse.put("etapesCalcul", java.util.List.of());
        reponse.put("avertissements", java.util.List.of());

        Optional<ResultatCalculFiscal> resultat = calculerImpotDeclarationUseCase.execute(
                obligation.getCodeImpot(), chiffreAffaires, obligation.getNif());

        if (resultat.isPresent()) {
            ResultatCalculFiscal r = resultat.get();
            reponse.put("disponible", true);
            reponse.put("impotRetenu", r.getSolde() == null ? BigDecimal.ZERO : r.getSolde());
            reponse.put("baseImposable", r.getBaseImposable());
            reponse.put("tauxApplique", r.getTauxApplique());
            reponse.put("montantBrut", r.getMontantBrut());
            reponse.put("montantMinimum", r.getMontantMinimum());
            reponse.put("explication", r.getExplication());
            reponse.put("etapesCalcul", r.getEtapesCalcul());
            reponse.put("avertissements", r.getAvertissements());
        }

        return reponse;
    }
}
