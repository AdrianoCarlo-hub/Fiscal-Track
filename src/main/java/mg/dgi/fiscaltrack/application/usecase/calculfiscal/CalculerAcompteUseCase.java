package mg.dgi.fiscaltrack.application.usecase.calculfiscal;

import mg.dgi.fiscaltrack.application.usecase.admin.ParametreFiscalService;
import mg.dgi.fiscaltrack.domain.exception.CalculFiscalInvalideException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Calcul des acomptes provisionnels d'IR et d'IS.
 *
 * Regles (Document IR 2026, IS 2026) :
 *   - 2 acomptes provisionnels de 25% chacun de l'impot N-1 (total 50%)
 *     - 1er acompte : 31 mars
 *     - 2eme acompte : 30 juin
 *   - Acomptes forfaitaires de debut d'activite.
 *   - Pour cloture decalee : fin du 3e et du 6e mois
 */
@Service
public class CalculerAcompteUseCase {

    private final ParametreFiscalService parametreFiscalService;

    public CalculerAcompteUseCase(ParametreFiscalService parametreFiscalService) {
        this.parametreFiscalService = parametreFiscalService;
    }

    /**
     * Calcule un acompte provisionnel (25% de l'impot N-1).
     * Document IR 2026 : 2 acomptes de 25% chacun.
     */
    public BigDecimal calculerAcompteProvisionnel(BigDecimal impotN1) {
        if (impotN1 == null || impotN1.signum() < 0) {
            throw new CalculFiscalInvalideException("L'impot N-1 est obligatoire et positif");
        }
        // Taux de 25% (la moitie de 50% total)
        BigDecimal tauxAcompte = parametreFiscalService.getValeur("ACOMPTE_TAUX_PROVISIONNEL")
                .divide(new BigDecimal(2), 4, java.math.RoundingMode.HALF_UP);
        return impotN1.multiply(tauxAcompte);
    }

    /**
     * Acomptes de debut d'activite pour IS.
     * Document IS 2026.
     */
    public BigDecimal calculerAcompteDebutActiviteIs(String categorie) {
        if (categorie == null) {
            throw new CalculFiscalInvalideException("La categorie est obligatoire");
        }
        String cat = categorie.toUpperCase();
        
        // AGRICOLE, ELEVEUR
        if (cat.equals("AGRICOLE") || cat.equals("ELEVEUR")) {
            return parametreFiscalService.getValeur("ACOMPTE_IS_AGRICOLE_ELEVEUR");
        }
        // ARTISAN, GARGOTIER, PETIT_COMMERCANT
        if (cat.equals("ARTISAN") || cat.equals("GARGOTIER") || cat.equals("PETIT_COMMERCANT")) {
            return parametreFiscalService.getValeur("ACOMPTE_IS_ARTISAN");
        }
        // COMMERCANT, HOTELIER, PRESTATAIRE
        if (cat.equals("COMMERCANT") || cat.equals("HOTELIER") || cat.equals("PRESTATAIRE")) {
            return parametreFiscalService.getValeur("ACOMPTE_IS_COMMERCANT");
        }
        // PROFESSION_LIBERALE, ACTIVITES_MULTIPLES
        if (cat.equals("PROFESSION_LIBERALE") || cat.equals("ACTIVITES_MULTIPLES")) {
            return parametreFiscalService.getValeur("ACOMPTE_IS_PROFESSION_LIBERALE");
        }
        // Default
        return parametreFiscalService.getValeur("ACOMPTE_IS_ARTISAN");
    }

    /**
     * Acomptes de debut d'activite pour IR.
     * Document IR 2026.
     */
    public BigDecimal calculerAcompteDebutActiviteIr(String categorie) {
        if (categorie == null) {
            throw new CalculFiscalInvalideException("La categorie est obligatoire");
        }
        String cat = categorie.toUpperCase();
        if (cat.equals("INDUSTRIE") || cat.equals("ARTISANAT")
                || cat.equals("AGRICULTURE") || cat.equals("TOURISME")) {
            return parametreFiscalService.getValeur("ACOMPTE_IR_INDUSTRIE_ETC");
        }
        return parametreFiscalService.getValeur("ACOMPTE_IR_AUTRES");
    }
}
