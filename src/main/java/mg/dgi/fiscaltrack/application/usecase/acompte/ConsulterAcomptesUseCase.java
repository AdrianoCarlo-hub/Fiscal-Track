package mg.dgi.fiscaltrack.application.usecase.acompte;

import mg.dgi.fiscaltrack.application.port.out.CompteCourantFiscalRepositoryPort;
import mg.dgi.fiscaltrack.application.port.out.DeclarationRepositoryPort;
import mg.dgi.fiscaltrack.application.port.out.ObligationFiscaleRepositoryPort;
import mg.dgi.fiscaltrack.domain.model.CompteCourantFiscal;
import mg.dgi.fiscaltrack.domain.model.Declaration;
import mg.dgi.fiscaltrack.domain.model.ObligationFiscale;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Liste les acomptes obligatoires d'un contribuable pour un exercice.
 * Le montant est recalcule a chaque ouverture de la page (le serveur fait foi)
 * et l'apercu stocke est mis a jour.
 */
@Service
public class ConsulterAcomptesUseCase {

    private final ObligationFiscaleRepositoryPort obligationRepositoryPort;
    private final DeclarationRepositoryPort declarationRepositoryPort;
    private final CompteCourantFiscalRepositoryPort compteRepositoryPort;
    private final AcomptesObligatoiresService acomptesService;

    public ConsulterAcomptesUseCase(ObligationFiscaleRepositoryPort obligationRepositoryPort,
                                    DeclarationRepositoryPort declarationRepositoryPort,
                                    CompteCourantFiscalRepositoryPort compteRepositoryPort,
                                    AcomptesObligatoiresService acomptesService) {
        this.obligationRepositoryPort = obligationRepositoryPort;
        this.declarationRepositoryPort = declarationRepositoryPort;
        this.compteRepositoryPort = compteRepositoryPort;
        this.acomptesService = acomptesService;
    }

    public List<Map<String, Object>> execute(String nif, int exercice) {
        List<Map<String, Object>> resultat = new ArrayList<>();
        List<ObligationFiscale> obligations = obligationRepositoryPort.findByNif(nif).stream()
                .filter(o -> o.getCodeImpot() != null
                        && o.getCodeImpot().toUpperCase().startsWith("ACOMPTE_"))
                .filter(o -> exerciceDe(o.getPeriodeFiscale()) == exercice)
                .sorted((a, b) -> String.valueOf(a.getPeriodeFiscale())
                        .compareTo(String.valueOf(b.getPeriodeFiscale())))
                .toList();

        for (ObligationFiscale obligation : obligations) {
            AcomptesObligatoiresService.CalculAcompte calcul =
                    acomptesService.calculerPourObligation(obligation);
            if (calcul.disponible()) {
                acomptesService.mettreAJourApercu(obligation, calcul);
                obligationRepositoryPort.save(obligation);
            }

            BigDecimal montantDu = calcul.disponible()
                    ? calcul.montant() : obligation.getMontantAcompte();
            BigDecimal montantPaye = declarationRepositoryPort
                    .findByIdObligationFiscale(obligation.getIdObligationFiscale())
                    .flatMap(d -> compteRepositoryPort.findByIdDeclaration(d.getIdDeclaration()))
                    .map(CompteCourantFiscal::getMontantPaye)
                    .orElse(BigDecimal.ZERO);

            Map<String, Object> ligne = new LinkedHashMap<>();
            ligne.put("idObligationFiscale", obligation.getIdObligationFiscale());
            ligne.put("nif", obligation.getNif());
            ligne.put("codeImpot", obligation.getCodeImpot());
            ligne.put("periodeFiscale", obligation.getPeriodeFiscale());
            ligne.put("dateLimiteReelle", obligation.getDateLimiteReelle());
            ligne.put("statutDeclaration", obligation.getStatutDeclaration() == null
                    ? null : obligation.getStatutDeclaration().name());
            ligne.put("disponible", calcul.disponible());
            ligne.put("montantAcompte", montantDu);
            ligne.put("montantPaye", montantPaye);
            ligne.put("baseAcompte", calcul.base());
            ligne.put("tauxAcompte", calcul.taux());
            ligne.put("exerciceBase", calcul.exerciceBase());
            ligne.put("message", calcul.message());
            ligne.put("avertissements", calcul.avertissements());
            ligne.put("detail", calcul.detail());
            resultat.add(ligne);
        }
        return resultat;
    }

    private static int exerciceDe(String periode) {
        if (periode == null || periode.length() < 4) {
            return 0;
        }
        try {
            return Integer.parseInt(periode.substring(0, 4));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
