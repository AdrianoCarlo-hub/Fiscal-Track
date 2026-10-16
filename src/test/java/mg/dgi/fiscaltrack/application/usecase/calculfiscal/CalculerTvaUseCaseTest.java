package mg.dgi.fiscaltrack.application.usecase.calculfiscal;

import mg.dgi.fiscaltrack.application.usecase.admin.ParametreFiscalService;
import mg.dgi.fiscaltrack.domain.model.ResultatCalculFiscal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class CalculerTvaUseCaseTest {

    private CalculerTvaUseCase useCase;
    private ParametreFiscalService parametreFiscalService;

    @BeforeEach
    void setUp() {
        parametreFiscalService = Mockito.mock(ParametreFiscalService.class);
        useCase = new CalculerTvaUseCase(parametreFiscalService);

        // Mock des parametres fiscaux pour TVA
        Mockito.when(parametreFiscalService.getValeur("TVA_TAUX_NORMAL"))
                .thenReturn(new BigDecimal("0.20"));
        Mockito.when(parametreFiscalService.getValeur("TVA_TAUX_EXPORT"))
                .thenReturn(new BigDecimal("0.00"));
    }

    @Test
    @DisplayName("TVA positive : collectee > deductible")
    void tvaPositive() {
        ResultatCalculFiscal r = useCase.execute(
                new BigDecimal("1000000"),
                new BigDecimal("400000"),
                BigDecimal.ZERO,
                false);

        // TVA nette = 600 000
        assertEquals(0, r.getSolde().compareTo(new BigDecimal("600000")));
        assertEquals(0, r.getCreditFiscal().compareTo(BigDecimal.ZERO));
    }

    @Test
    @DisplayName("TVA negative : credit de TVA genere")
    void tvaCredit() {
        ResultatCalculFiscal r = useCase.execute(
                new BigDecimal("300000"),
                new BigDecimal("500000"),
                BigDecimal.ZERO,
                false);

        // TVA nette = -200 000 -> credit 200 000
        assertEquals(0, r.getSolde().compareTo(BigDecimal.ZERO));
        assertEquals(0, r.getCreditFiscal().compareTo(new BigDecimal("200000")));
    }

    @Test
    @DisplayName("TVA avec credit anterieur")
    void tvaCreditAnterieur() {
        ResultatCalculFiscal r = useCase.execute(
                new BigDecimal("500000"),
                new BigDecimal("300000"),
                new BigDecimal("50000"),
                false);

        // TVA nette = 200k - 50k = 150k
        assertEquals(0, r.getSolde().compareTo(new BigDecimal("150000")));
    }

    @Test
    @DisplayName("TVA export : taux 0%")
    void tvaExport() {
        ResultatCalculFiscal r = useCase.execute(
                new BigDecimal("1000000"),
                new BigDecimal("400000"),
                BigDecimal.ZERO,
                true);

        assertEquals(0, r.getTauxApplique().compareTo(new BigDecimal("0.00")));
        assertEquals(0, r.getSolde().compareTo(new BigDecimal("600000")));
    }
}
