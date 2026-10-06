package mg.dgi.fiscaltrack.application.usecase.calculfiscal;

import mg.dgi.fiscaltrack.domain.exception.CalculFiscalInvalideException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Calcul des acomptes provisionnels d'IR et d'IS.
 *
 * Regles :
 *   - 2 acomptes egaux de 50 % de l'impot N-1 (15 mai et 15 novembre).
 *   - Acomptes forfaitaires de debut d'activite.
 */
@Service
public class CalculerAcompteUseCase {

    public BigDecimal calculerAcompteProvisionnel(BigDecimal impotN1) {
        if (impotN1 == null || impotN1.signum() < 0) {
            throw new CalculFiscalInvalideException("L'impot N-1 est obligatoire et positif");
        }
        return impotN1.multiply(new BigDecimal("0.50"));
    }

    public BigDecimal calculerAcompteDebutActiviteIs(String categorie) {
        if (categorie == null) {
            throw new CalculFiscalInvalideException("La categorie est obligatoire");
        }
        switch (categorie.toUpperCase()) {
            case "AGRICOLE":
            case "ELEVEUR":
                return new BigDecimal("16000");
            case "ARTISAN":
            case "GARGOTIER":
            case "PETIT_COMMERCANT":
                return new BigDecimal("50000");
            case "COMMERCANT":
            case "HOTELIER":
            case "PRESTATAIRE":
                return new BigDecimal("100000");
            case "PROFESSION_LIBERALE":
            case "ACTIVITES_MULTIPLES":
                return new BigDecimal("150000");
            default:
                return new BigDecimal("50000");
        }
    }

    public BigDecimal calculerAcompteDebutActiviteIr(String categorie) {
        if (categorie == null) {
            throw new CalculFiscalInvalideException("La categorie est obligatoire");
        }
        String cat = categorie.toUpperCase();
        if (cat.equals("INDUSTRIE") || cat.equals("ARTISANAT")
                || cat.equals("AGRICULTURE") || cat.equals("TOURISME")) {
            return new BigDecimal("500000");
        }
        return new BigDecimal("1000000");
    }
}
