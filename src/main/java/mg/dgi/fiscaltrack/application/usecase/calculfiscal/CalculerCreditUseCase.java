package mg.dgi.fiscaltrack.application.usecase.calculfiscal;

import mg.dgi.fiscaltrack.domain.exception.CalculFiscalInvalideException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Calcul du credit d'impot lorsqu'un contribuable a verse
 * plus d'acomptes que l'impot reellement du.
 */
@Service
public class CalculerCreditUseCase {

    /**
     * Retourne le credit fiscal (>= 0).
     *   credit = MAX(0, acomptes - impotDu)
     */
    public BigDecimal execute(BigDecimal impotDu, BigDecimal acomptes) {
        if (impotDu == null) {
            throw new CalculFiscalInvalideException("L'impot du est obligatoire");
        }
        if (acomptes == null) {
            acomptes = BigDecimal.ZERO;
        }
        BigDecimal difference = acomptes.subtract(impotDu);
        return difference.signum() > 0 ? difference : BigDecimal.ZERO;
    }
}
